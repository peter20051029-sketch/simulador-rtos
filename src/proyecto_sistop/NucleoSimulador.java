/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

import java.util.concurrent.Semaphore;

public class NucleoSimulador implements Runnable {

    private volatile boolean corriendo;
    private long sumaEsperaTerminados;

    private java.util.function.Consumer<SnapshotSistema> listener;

    private boolean modoKernel;

    // ===== NUEVO: si está activo, NO se generan procesos random =====
    private boolean modoCSV;

    private long tick;
    private int duracionCicloMs;

    private final ListaEnlazada<BCP> colaListoSuspendido;
    private final ListaEnlazada<BCP> colaBloqueadoSuspendido;

    private final int MAX_EN_MEMORIA;

    private final ListaEnlazada<BCP> colaNuevo;
    private final ListaEnlazada<BCP> colaListo;
    private final ListaEnlazada<BCP> colaBloqueado;
    private final ListaEnlazada<BCP> colaTerminado;

    private final ListaEnlazada<BCP> colaEmergencia;
    private int contadorEmergencias;

    private final Semaphore mutexColas;

    private final GeneradorProcesos generador;
    private PoliticaPlanificacion politica;

    private BCP ejecutando;
    private int quantumRestante;

    private long ticksCpuOcupada;
    private int terminadosTotales;
    private int terminadosAntesDeadline;
    private int deadlinesFallados;
    private int interrupciones;

    private StringBuilder log;

    public NucleoSimulador(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
        this.tick = 0;
        this.corriendo = false;
        this.modoKernel = true;

        this.modoCSV = false;

        this.sumaEsperaTerminados = 0;

        this.colaEmergencia = new ListaEnlazada<>();
        this.contadorEmergencias = 0;

        this.colaListoSuspendido = new ListaEnlazada<>();
        this.colaBloqueadoSuspendido = new ListaEnlazada<>();
        this.MAX_EN_MEMORIA = 15;

        this.colaNuevo = new ListaEnlazada<>();
        this.colaListo = new ListaEnlazada<>();
        this.colaBloqueado = new ListaEnlazada<>();
        this.colaTerminado = new ListaEnlazada<>();

        this.mutexColas = new Semaphore(1, true);

        this.generador = new GeneradorProcesos();
        this.politica = new PlanificadorFCFS();

        this.ejecutando = null;
        this.quantumRestante = 0;

        this.ticksCpuOcupada = 0;
        this.terminadosTotales = 0;
        this.terminadosAntesDeadline = 0;
        this.deadlinesFallados = 0;
        this.interrupciones = 0;

        this.log = new StringBuilder();
        this.listener = null;
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
    }

    // ===== NUEVO: activar/desactivar modo CSV =====
    public void setModoCSV(boolean activo) {
        try {
            mutexColas.acquire();
            this.modoCSV = activo;
            logEvento("Modo CSV = " + (activo ? "ACTIVO (sin generación random)" : "INACTIVO"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    // ================== CARGA EXTERNA (CSV) ==================

    public void limpiarColas() {
        try {
            mutexColas.acquire();
            vaciarCola(colaNuevo);
            vaciarCola(colaListo);
            vaciarCola(colaBloqueado);
            vaciarCola(colaTerminado);
            vaciarCola(colaEmergencia);
            vaciarCola(colaListoSuspendido);
            vaciarCola(colaBloqueadoSuspendido);
            ejecutando = null;
            quantumRestante = 0;
            logEvento("Colas limpiadas (pre-carga desde archivo)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    public int cargarDesdeLista(ListaEnlazada<BCP> procesos) {
        int count = 0;
        if (procesos == null) return 0;

        try {
            mutexColas.acquire();
            modoKernel = true;

            // Al cargar desde CSV, activamos modoCSV automáticamente
            this.modoCSV = true;

            while (!procesos.estaVacia()) {
                BCP p = procesos.desencolar();
                if (p == null) break;
                p.setEstado(EstadoProceso.NUEVO);
                colaNuevo.encolar(p);
                count++;
            }

            logEvento("Carga desde archivo: " + count + " procesos (Modo CSV activo)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }

        return count;
    }

    // ======== Generación de procesos (BLOQUEADA en modo CSV) ========

    public void agregarProcesoAleatorioCPU() { agregarProcesoAleatorio(0); }
    public void agregarProcesoAleatorioIO() { agregarProcesoAleatorio(1); }
    public void agregarProcesoAleatorioMixto() { agregarProcesoAleatorio(2); }

    private void agregarProcesoAleatorio(int tipo) {
        try {
            mutexColas.acquire();
            modoKernel = true;

            if (modoCSV) {
                logEvento("Generación random bloqueada (Modo CSV activo)");
                return;
            }

            generador.generar(1, tick);
            ListaEnlazada<BCP> nuevos = generador.obtener();
            if (nuevos.estaVacia()) return;

            BCP p = nuevos.desencolar();

            if (tipo == 0) {
                p.setRafagaESRestante(0);
            } else if (tipo == 1) {
                p.setRafagaESRestante(Math.max(p.getRafagaESRestante(), 15));
            } else {
                p.setRafagaESRestante(Math.max(p.getRafagaESRestante(), 6));
            }

            p.setEstado(EstadoProceso.NUEVO);
            colaNuevo.encolar(p);

            logEvento("Nuevo proceso agregado: " + p.getNombre()
                    + " (tipo=" + (tipo == 0 ? "CPU" : tipo == 1 ? "IO" : "MIXTO") + ")");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    public void cargarInicial(int cantidad) {
        try {
            mutexColas.acquire();

            if (modoCSV) {
                logEvento("cargarInicial(" + cantidad + ") bloqueado (Modo CSV activo)");
                return;
            }

            generador.generar(cantidad, tick);
            ListaEnlazada<BCP> nuevos = generador.obtener();
            while (!nuevos.estaVacia()) colaNuevo.encolar(nuevos.desencolar());

            logEvento("Carga random inicial: " + cantidad + " procesos");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    // ======== Interrupción / Emergencia ========

    public void dispararInterrupcion() {
        try {
            mutexColas.acquire();
            modoKernel = true;
            interrupciones++;

            contadorEmergencias++;
            BCP emergencia = new BCP(
                    "EMERG-" + contadorEmergencias,
                    1,
                    tick + 50,
                    10,
                    0,
                    null
            );

            logEvento("Interrupción detectada: " + emergencia.getNombre());

            emergencia.setEstado(EstadoProceso.LISTO);
            colaEmergencia.encolar(emergencia);

            if (ejecutando != null) {
                ejecutando.setEstado(EstadoProceso.LISTO);
                colaListo.encolar(ejecutando);
                ejecutando = null;
                quantumRestante = 0;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        try {
            mutexColas.acquire();
            this.politica = politica;
            reordenarColaListos();
            if (!(politica instanceof PlanificadorRR)) quantumRestante = 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }
    
    public void resetearSistema(int maxProcesosIniciales) {
    try {
        mutexColas.acquire();
        corriendo = false;

        vaciarCola(colaNuevo);
        vaciarCola(colaListo);
        vaciarCola(colaEmergencia);
        vaciarCola(colaBloqueado);
        vaciarCola(colaTerminado);
        vaciarCola(colaListoSuspendido);
        vaciarCola(colaBloqueadoSuspendido);

        ejecutando = null;
        quantumRestante = 0;

        tick = 0;

        ticksCpuOcupada = 0;
        terminadosTotales = 0;
        terminadosAntesDeadline = 0;
        deadlinesFallados = 0;
        interrupciones = 0;
        sumaEsperaTerminados = 0;

        log.setLength(0);

        // al reset, volvemos a random (para no quedar "trancado" en modo CSV)
        modoCSV = false;

        // vuelve a cargar procesos iniciales random
        cargarInicial(maxProcesosIniciales);
        logEvento("Reset del sistema (" + maxProcesosIniciales + " procesos)");

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        mutexColas.release();
    }
}

    private String lineaPCB(BCP p) {
        long dRem = p.getTiempoLimite() - tick;
        return "ID=" + p.getId()
                + " | " + p.getNombre()
                + " | " + p.getEstado()
                + " | PC=" + p.getContadorPrograma()
                + " | MAR=" + p.getRegistroDireccionMemoria()
                + " | Prio=" + p.getPrioridad()
                + " | DRem=" + dRem;
    }
    
    public String obtenerLogTexto() {
    try {
        mutexColas.acquire();
        return log.toString();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return "";
    } finally {
        mutexColas.release();
    }
}
    
    private String textoCola(ListaEnlazada<BCP> cola) {
        StringBuilder sb = new StringBuilder();
        cola.paraCada(p -> sb.append(lineaPCB(p)).append("\n"));
        return sb.toString();
    }

    private void acumularEsperaEnReady() {
        colaListo.paraCada(p -> p.sumarEspera(1));
    }

    public void setListener(java.util.function.Consumer<SnapshotSistema> listener) {
        this.listener = listener;
    }

    private int procesosEnMemoria() {
        int enCpu = (ejecutando == null) ? 0 : 1;
        return colaListo.tamano() + colaBloqueado.tamano() + enCpu;
    }

    private void meterAListoO_Suspender(BCP p) {
        if (p.getTickLlegada() == 0) p.setTickLlegada(tick);

        if (procesosEnMemoria() < MAX_EN_MEMORIA) {
            p.setEstado(EstadoProceso.LISTO);
            insertarEnListosSegunPolitica(p);
        } else {
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            colaListoSuspendido.encolar(p);
            logEvento("Proceso movido a Suspendido: " + p.getNombre());
        }
    }

    private void meterABloqueadoO_Suspender(BCP p) {
        if (procesosEnMemoria() < MAX_EN_MEMORIA) {
            p.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueado.encolar(p);
        } else {
            p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            colaBloqueadoSuspendido.encolar(p);
            logEvento("Proceso movido a Bloqueado-Suspendido: " + p.getNombre());
        }
    }

    private void swapInSiHayEspacio() {
        while (procesosEnMemoria() < MAX_EN_MEMORIA) {
            if (!colaListoSuspendido.estaVacia()) {
                BCP p = colaListoSuspendido.desencolar();
                p.setEstado(EstadoProceso.LISTO);
                insertarEnListosSegunPolitica(p);
                logEvento("Swap-In (Listo): " + p.getNombre());
            } else if (!colaBloqueadoSuspendido.estaVacia()) {
                BCP p = colaBloqueadoSuspendido.desencolar();
                p.setEstado(EstadoProceso.BLOQUEADO);
                colaBloqueado.encolar(p);
                logEvento("Swap-In (Bloqueado): " + p.getNombre());
            } else break;
        }
    }

    private void reordenarColaListos() {
        ListaEnlazada<BCP> tmp = new ListaEnlazada<>();
        while (!colaListo.estaVacia()) tmp.encolar(colaListo.desencolar());
        while (!tmp.estaVacia()) insertarEnListosSegunPolitica(tmp.desencolar());
    }

    private void logEvento(String msg) {
        log.append("[").append(tick).append("] ").append(msg).append("\n");
    }

    private SnapshotSistema crearSnapshot() {
        SnapshotSistema s = new SnapshotSistema();

        s.tick = tick;
        s.politica = politica.nombre();
        s.ejecutando = (ejecutando == null) ? "Ninguno" : (
                ejecutando.getNombre()
                        + " | prio=" + ejecutando.getPrioridad()
                        + " | rest=" + ejecutando.getInstruccionesRestantes()
                        + " | lim=" + ejecutando.getTiempoLimite()
                        + " | PC=" + ejecutando.getContadorPrograma()
                        + " | MAR=" + ejecutando.getRegistroDireccionMemoria()
                        + " | DRem=" + (ejecutando.getTiempoLimite() - tick)
        );

        s.nuevo = colaNuevo.tamano();
        s.listo = colaListo.tamano();
        s.emergencia = colaEmergencia.tamano();
        s.bloqueado = colaBloqueado.tamano();
        s.terminado = colaTerminado.tamano();
        s.listoSusp = colaListoSuspendido.tamano();
        s.bloqSusp = colaBloqueadoSuspendido.tamano();

        s.cpuUtil = (tick == 0) ? 0.0 : ((double) ticksCpuOcupada / (double) tick) * 100.0;
        s.tasaExito = (terminadosTotales == 0) ? 0.0 : ((double) terminadosAntesDeadline / (double) terminadosTotales) * 100.0;

        s.deadlinesFallados = deadlinesFallados;
        s.terminadosTotales = terminadosTotales;
        s.interrupciones = interrupciones;

        s.log = log.toString();
        s.modoKernel = modoKernel;

        s.txtNuevo = textoCola(colaNuevo);
        s.txtListo = textoCola(colaListo);
        s.txtBloqueado = textoCola(colaBloqueado);
        s.txtTerminado = textoCola(colaTerminado);
        s.txtListoSusp = textoCola(colaListoSuspendido);
        s.txtBloqSusp = textoCola(colaBloqueadoSuspendido);
        s.txtEmergencia = textoCola(colaEmergencia);

        s.throughput = (tick == 0) ? 0.0 : ((double) terminadosTotales / (double) tick);
        s.esperaPromedio = (terminadosTotales == 0) ? 0.0 : ((double) sumaEsperaTerminados / (double) terminadosTotales);

        return s;
    }

    private void insertarEnListosSegunPolitica(BCP p) {
        if (politica instanceof PlanificadorSRT) {
            colaListo.insertarOrdenado(p, PlanificadorSRT.COMP);
        } else if (politica instanceof PlanificadorPrioridad) {
            colaListo.insertarOrdenado(p, PlanificadorPrioridad.COMP);
        } else if (politica instanceof PlanificadorEDF) {
            colaListo.insertarOrdenado(p, PlanificadorEDF.COMP);
        } else {
            colaListo.encolar(p);
        }
    }

    private void preemptarSiCorresponde() {
        if (ejecutando == null) return;
        if (politica instanceof PlanificadorRR || politica instanceof PlanificadorFCFS) return;

        BCP candidato = colaListo.verPrimero();
        if (candidato == null) return;

        boolean preemptar = false;

        if (politica instanceof PlanificadorSRT) {
            preemptar = candidato.getInstruccionesRestantes() < ejecutando.getInstruccionesRestantes();
        } else if (politica instanceof PlanificadorPrioridad) {
            preemptar = candidato.getPrioridad() < ejecutando.getPrioridad();
        } else if (politica instanceof PlanificadorEDF) {
            preemptar = candidato.getTiempoLimite() < ejecutando.getTiempoLimite();
        }

        if (preemptar) {
            ejecutando.setEstado(EstadoProceso.LISTO);
            insertarEnListosSegunPolitica(ejecutando);
            ejecutando = null;
            quantumRestante = 0;
            logEvento("Cambio de contexto (preempción)");
        }
    }

    public void iniciar() { this.corriendo = true; }
    public void detener() { this.corriendo = false; }

    private void admitirNuevosAListos() {
        while (!colaNuevo.estaVacia()) {
            BCP p = colaNuevo.desencolar();
            meterAListoO_Suspender(p);
        }
    }

    private void despacharSiHaceFalta() {
        if (ejecutando != null) return;

        BCP siguiente;
        if (!colaEmergencia.estaVacia()) siguiente = colaEmergencia.desencolar();
        else siguiente = politica.elegirSiguiente(colaListo);

        if (siguiente == null) return;

        siguiente.setEstado(EstadoProceso.EJECUCION);
        ejecutando = siguiente;

        if (politica instanceof PlanificadorRR rr) quantumRestante = rr.getQuantum();
        else quantumRestante = 0;
    }

    private void aplicarRRSiToca() {
        if (!(politica instanceof PlanificadorRR)) return;
        if (ejecutando == null) return;

        quantumRestante--;

        if (quantumRestante <= 0 && ejecutando.getInstruccionesRestantes() > 0) {
            ejecutando.setEstado(EstadoProceso.LISTO);
            colaListo.encolar(ejecutando);
            ejecutando = null;
        }
    }

    // ✅ ESTE MÉTODO FALTABA EN TU ARCHIVO (por eso daba error)
    private void ejecutarUnCiclo() {
        if (ejecutando == null) return;

        // Si tiene E/S pendiente, puede bloquearse
        if (ejecutando.getRafagaESRestante() > 0) {
            int r = (int) (Math.random() * 100);
            if (r < 70) {
                ejecutando.setEstado(EstadoProceso.BLOQUEADO);
                meterABloqueadoO_Suspender(ejecutando);
                ejecutando = null;
                return;
            }
        }

        ejecutando.setContadorPrograma(ejecutando.getContadorPrograma() + 1);
        ejecutando.setRegistroDireccionMemoria(ejecutando.getRegistroDireccionMemoria() + 1);
        ejecutando.decrementarInstruccion();

        if (ejecutando.getInstruccionesRestantes() <= 0) {
            ejecutando.setEstado(EstadoProceso.TERMINADO);

            terminadosTotales++;
            if (tick <= ejecutando.getTiempoLimite()) terminadosAntesDeadline++;
            else {
                deadlinesFallados++;
                logEvento("Fallo de deadline en " + ejecutando.getNombre());
            }

            sumaEsperaTerminados += ejecutando.getEsperaAcumulada();
            colaTerminado.encolar(ejecutando);
            ejecutando = null;
        }
    }

    private void vaciarCola(ListaEnlazada<BCP> cola) {
        while (!cola.estaVacia()) cola.desencolar();
    }

    // ✅ CAMBIO: que Bloqueado no muera con el tiempo (múltiples ráfagas E/S)
    private void avanzarBloqueados() {
        int n = colaBloqueado.tamano();
        for (int i = 0; i < n; i++) {
            BCP p = colaBloqueado.desencolar();
            if (p == null) break;

            if (p.getRafagaESRestante() > 0) p.setRafagaESRestante(p.getRafagaESRestante() - 1);

            if (p.getRafagaESRestante() <= 0) {
                p.setEstado(EstadoProceso.LISTO);

                if (Math.random() < 0.35) {
                    p.setRafagaESRestante(5 + (int)(Math.random() * 8)); // 5..12
                    logEvento("Proceso con nueva ráfaga de E/S: " + p.getNombre());
                }

                meterAListoO_Suspender(p);
            } else {
                colaBloqueado.encolar(p);
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (corriendo) {
                try {
                    mutexColas.acquire();
                    tick++;

                    // ===== KERNEL (SO): colas + planificación =====
                    modoKernel = true;
                    if (ejecutando != null) ticksCpuOcupada++;

                    acumularEsperaEnReady();
                    admitirNuevosAListos();
                    avanzarBloqueados();
                    preemptarSiCorresponde();
                    despacharSiHaceFalta();

                    if (listener != null) listener.accept(crearSnapshot());

                    // ===== USUARIO: ejecutar 1 instrucción =====
                    modoKernel = false;
                    ejecutarUnCiclo();

                    if (listener != null) listener.accept(crearSnapshot());

                    // ===== KERNEL: RR + swap =====
                    modoKernel = true;
                    aplicarRRSiToca();
                    swapInSiHayEspacio();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    mutexColas.release();
                }
            }

            try {
                Thread.sleep(duracionCicloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}