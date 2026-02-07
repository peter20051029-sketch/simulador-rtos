/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

public class NucleoSimulador implements Runnable {

    private volatile boolean corriendo;

    private long tick;
    private int duracionCicloMs;

    private final ListaEnlazada<BCP> colaNuevo;
    private final ListaEnlazada<BCP> colaListo;
    private final ListaEnlazada<BCP> colaBloqueado;
    private final ListaEnlazada<BCP> colaTerminado;

    private final GeneradorProcesos generador;
    private PoliticaPlanificacion politica;

    private BCP ejecutando;

    public NucleoSimulador(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
        this.tick = 0;
        this.corriendo = false;

        this.colaNuevo = new ListaEnlazada<>();
        this.colaListo = new ListaEnlazada<>();
        this.colaBloqueado = new ListaEnlazada<>();
        this.colaTerminado = new ListaEnlazada<>();

        this.generador = new GeneradorProcesos();
        this.politica = new PlanificadorFCFS();
        this.ejecutando = null;
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
    }

    public void iniciar() {
        this.corriendo = true;
    }

    public void detener() {
        this.corriendo = false;
    }

    public void cargarInicial(int cantidad) {
        generador.generar(cantidad, tick);
        ListaEnlazada<BCP> nuevos = generador.obtener();
        while (!nuevos.estaVacia()) {
            colaNuevo.encolar(nuevos.desencolar());
        }
    }

    private void admitirNuevosAListos() {
        while (!colaNuevo.estaVacia()) {
            BCP p = colaNuevo.desencolar();
            p.setEstado(EstadoProceso.LISTO);
            colaListo.encolar(p);
        }
    }

    private void despacharSiHaceFalta() {
        if (ejecutando == null) {
            BCP siguiente = politica.elegirSiguiente(colaListo);
            if (siguiente != null) {
                siguiente.setEstado(EstadoProceso.EJECUCION);
                ejecutando = siguiente;
            }
        }
    }

    private void ejecutarUnCiclo() {
        if (ejecutando == null) return;

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
        System.out.println("\nTick: " + tick + " | Politica: " + politica.nombre());
        System.out.println("Ejecutando: " + (ejecutando == null ? "Ninguno" : ejecutando.getNombre()
                + " (restantes=" + ejecutando.getInstruccionesRestantes() + ", limite=" + ejecutando.getTiempoLimite() + ")"));
        System.out.println("Listos: " + colaListo.tamano() + " | Nuevo: " + colaNuevo.tamano()
                + " | Bloqueado: " + colaBloqueado.tamano() + " | Terminado: " + colaTerminado.tamano());
    }

    @Override
    public void run() {
        while (true) {
            if (corriendo) {
                tick++;

                admitirNuevosAListos();
                despacharSiHaceFalta();
                ejecutarUnCiclo();
                imprimirEstado();
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
