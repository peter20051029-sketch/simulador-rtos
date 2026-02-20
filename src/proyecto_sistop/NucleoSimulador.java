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


    private final Semaphore mutexColas; // mutex

    private final GeneradorProcesos generador;
    private PoliticaPlanificacion politica;

    private BCP ejecutando;
    private int quantumRestante; // RR

    public NucleoSimulador(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
        this.tick = 0;
        this.corriendo = false;
        this.colaEmergencia = new ListaEnlazada<>();
        this.contadorEmergencias = 0;
        this.colaListoSuspendido = new ListaEnlazada<>();
        this.colaBloqueadoSuspendido = new ListaEnlazada<>();

        this.MAX_EN_MEMORIA = 5; // cámbialo si quieres (o lo pasamos por constructor)


        this.colaNuevo = new ListaEnlazada<>();
        this.colaListo = new ListaEnlazada<>();
        this.colaBloqueado = new ListaEnlazada<>();
        this.colaTerminado = new ListaEnlazada<>();

        this.mutexColas = new Semaphore(1, true);

        this.generador = new GeneradorProcesos();
        this.politica = new PlanificadorFCFS();

        this.ejecutando = null;
        this.quantumRestante = 0;
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
    }
    
    public void dispararInterrupcion() {
    try {
        mutexColas.acquire();

        contadorEmergencias++;
        BCP emergencia = new BCP(
                "EMERG-" + contadorEmergencias,
                1,
                tick + 50,
                10,
                0,
                null
        );

        emergencia.setEstado(EstadoProceso.LISTO);
        colaEmergencia.encolar(emergencia);

        if (ejecutando != null) {
            ejecutando.setEstado(EstadoProceso.LISTO);
            colaListo.encolar(ejecutando);
            ejecutando = null;
            quantumRestante = 0;
        }

        System.out.println("\n*** INTERRUPCION: creada " + emergencia.getNombre() + " ***");

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
            if (!(politica instanceof PlanificadorRR)) {
                quantumRestante = 0;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }
    private int procesosEnMemoria() {
    int enCpu = (ejecutando == null) ? 0 : 1;
    return colaListo.tamano() + colaBloqueado.tamano() + enCpu;
}
private void meterAListoO_Suspender(BCP p) {
    if (procesosEnMemoria() < MAX_EN_MEMORIA) {
        p.setEstado(EstadoProceso.LISTO);
        insertarEnListosSegunPolitica(p);
    } else {
        p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
        colaListoSuspendido.encolar(p);
    }
}
private void meterABloqueadoO_Suspender(BCP p) {
    if (procesosEnMemoria() < MAX_EN_MEMORIA) {
        p.setEstado(EstadoProceso.BLOQUEADO);
        colaBloqueado.encolar(p);
    } else {
        p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
        colaBloqueadoSuspendido.encolar(p);
    }
}
private void swapInSiHayEspacio() {
    while (procesosEnMemoria() < MAX_EN_MEMORIA) {
        if (!colaListoSuspendido.estaVacia()) {
            BCP p = colaListoSuspendido.desencolar();
            p.setEstado(EstadoProceso.LISTO);
            insertarEnListosSegunPolitica(p);
        } else if (!colaBloqueadoSuspendido.estaVacia()) {
            BCP p = colaBloqueadoSuspendido.desencolar();
            p.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueado.encolar(p);
        } else {
            break;
        }
    }
}

    private void reordenarColaListos() {
    ListaEnlazada<BCP> tmp = new ListaEnlazada<>();
    while (!colaListo.estaVacia()) {
        tmp.encolar(colaListo.desencolar());
    }
    while (!tmp.estaVacia()) {
        BCP p = tmp.desencolar();
        insertarEnListosSegunPolitica(p);
    }
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


    public void iniciar() {
        this.corriendo = true;
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
    }
}
   
    
    public void detener() {
        this.corriendo = false;
    }

    public void cargarInicial(int cantidad) {
        try {
            mutexColas.acquire();
            generador.generar(cantidad, tick);
            ListaEnlazada<BCP> nuevos = generador.obtener();
            while (!nuevos.estaVacia()) {
                colaNuevo.encolar(nuevos.desencolar());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

   private void admitirNuevosAListos() {
    while (!colaNuevo.estaVacia()) {
        BCP p = colaNuevo.desencolar();
        meterAListoO_Suspender(p);
    }
}



   private void despacharSiHaceFalta() {
    if (ejecutando != null) return;

    BCP siguiente = null;

    if (!colaEmergencia.estaVacia()) {
        siguiente = colaEmergencia.desencolar();
    } else {
        siguiente = politica.elegirSiguiente(colaListo);
    }

    if (siguiente == null) return;

    siguiente.setEstado(EstadoProceso.EJECUCION);
    ejecutando = siguiente;

    if (politica instanceof PlanificadorRR rr) {
        quantumRestante = rr.getQuantum();
    } else {
        quantumRestante = 0;
    }
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

    private void ejecutarUnCiclo() {
    if (ejecutando == null) return;

    // chance simple de bloquearse (si tiene E/S pendiente)
    if (ejecutando.getRafagaESRestante() > 0) {
        int r = (int)(Math.random() * 100);
        if (r < 15) { // 15%
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
        colaTerminado.encolar(ejecutando);
        ejecutando = null;
    }
}


    private void imprimirEstado() {
        System.out.println("\nTick: " + tick + " | " + politica.nombre());

        if (ejecutando == null) {
            System.out.println("Ejecutando: Ninguno");
        } else {
            String extra = "";
            if (politica instanceof PlanificadorRR) extra = " | qRest=" + quantumRestante;
            System.out.println("Ejecutando: " + ejecutando.getNombre()
                    + " | rest=" + ejecutando.getInstruccionesRestantes()
                    + " | limite=" + ejecutando.getTiempoLimite()
                    + extra);
        }

        System.out.println("Nuevo=" + colaNuevo.tamano()
        + " | Listo=" + colaListo.tamano()
        + " | ListoSusp=" + colaListoSuspendido.tamano()
        + " | Emerg=" + colaEmergencia.tamano()
        + " | Bloqueado=" + colaBloqueado.tamano()
        + " | BloqSusp=" + colaBloqueadoSuspendido.tamano()
        + " | Terminado=" + colaTerminado.tamano());


    }
    
    private void avanzarBloqueados() {
    int n = colaBloqueado.tamano();
    for (int i = 0; i < n; i++) {
        BCP p = colaBloqueado.desencolar();
        if (p == null) break;

        if (p.getRafagaESRestante() > 0) {
            p.setRafagaESRestante(p.getRafagaESRestante() - 1);
        }

        if (p.getRafagaESRestante() <= 0) {
            p.setEstado(EstadoProceso.LISTO);
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

                    admitirNuevosAListos();
                    avanzarBloqueados();
                    preemptarSiCorresponde();
                    despacharSiHaceFalta();

                    ejecutarUnCiclo();
                    aplicarRRSiToca();
                    
                    swapInSiHayEspacio();
                    imprimirEstado();



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
