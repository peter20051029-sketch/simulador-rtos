package proyecto_sistop;

import java.util.concurrent.atomic.AtomicInteger;

public class BCP {
    /**
     * Contador atómico para generar identificadores únicos de proceso.
     */
    private static final AtomicInteger GENERADOR_ID = new AtomicInteger(0);

    /** Identificador único del proceso. */
    private final int id;
    /** Nombre del proceso */
    private String nombre;
    /** Estado actual del proceso. */
    private EstadoProceso estado;
    /** Prioridad del proceso (menor valor = mayor prioridad en algunas políticas). */
    private int prioridad;
    /** Tiempo límite absoluto (por ejemplo, ciclo de reloj en que debe finalizar). */
    private long tiempoLimite;
    /** Contador de programa: instrucción actual. */
    private int contadorPrograma;
    /** Registro de dirección de memoria: dirección actual en memoria. */
    private int registroDireccionMemoria;
    /** Número total de instrucciones por ejecutar. */
    private int instruccionesTotales;
    /** Número de instrucciones restantes por ejecutar. */
    private int instruccionesRestantes;
    /** Tiempo restante de ráfaga de E/S (si el proceso está bloqueado). */
    private int rafagaESRestante;
    /** Periodo de reactivación (para tareas periódicas), en ciclos de reloj. */
    private Integer periodo;
    private long esperaAcumulada;
    private long tickLlegada;

    /**
     * Crea un nuevo BCP con los parámetros indicados. El identificador se
     * genera automáticamente. El estado inicial será NUEVO y el contador de
     * programa y el registro de dirección se inicializan en cero.
     *
     * @param nombre nombre del proceso
     * @param prioridad prioridad asignada
     * @param tiempoLimite tiempo límite absoluto en ciclos de reloj
     * @param instruccionesTotales número de instrucciones totales
     * @param rafagaES tiempo de ráfaga de E/S inicial (puede ser cero)
     * @param periodo periodo de reactivación; puede ser null si no es periódico
     */
    public BCP(String nombre, int prioridad, long tiempoLimite, int instruccionesTotales, int rafagaES, Integer periodo) {
        this.id = GENERADOR_ID.getAndIncrement();
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.tiempoLimite = tiempoLimite;
        this.instruccionesTotales = instruccionesTotales;
        this.instruccionesRestantes = instruccionesTotales;
        this.rafagaESRestante = rafagaES;
        this.periodo = periodo;
        this.contadorPrograma = 0;
        this.registroDireccionMemoria = 0;
        this.estado = EstadoProceso.NUEVO;
        this.esperaAcumulada = 0;
        this.tickLlegada = 0;
    }


    /**
     * Devuelve el identificador único del proceso.
     * @return id del proceso
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el nombre del proceso.
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }
    public long getEsperaAcumulada() { return esperaAcumulada; }
public void sumarEspera(long t) { this.esperaAcumulada += t; }

public long getTickLlegada() { return tickLlegada; }
public void setTickLlegada(long tickLlegada) { this.tickLlegada = tickLlegada; }

    /**
     * Establece un nuevo nombre para el proceso.
     * @param nombre nuevo nombre
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve el estado actual del proceso.
     * @return estado del proceso
     */
    public EstadoProceso getEstado() {
        return estado;
    }

    /**
     * Establece el estado del proceso.
     * @param estado nuevo estado
     */
    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    /**
     * Devuelve la prioridad del proceso.
     * @return prioridad
     */
    public int getPrioridad() {
        return prioridad;
    }

    /**
     * Establece la prioridad del proceso.
     * @param prioridad nueva prioridad
     */
    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    /**
     * Devuelve el tiempo límite absoluto del proceso.
     * @return tiempo límite
     */
    public long getTiempoLimite() {
        return tiempoLimite;
    }

    /**
     * Establece un nuevo tiempo límite absoluto.
     * @param tiempoLimite nuevo tiempo límite
     */
    public void setTiempoLimite(long tiempoLimite) {
        this.tiempoLimite = tiempoLimite;
    }

    /**
     * Devuelve el valor actual del contador de programa.
     * @return contador de programa
     */
    public int getContadorPrograma() {
        return contadorPrograma;
    }

    /**
     * Establece el contador de programa a un nuevo valor.
     * @param contadorPrograma nuevo contador de programa
     */
    public void setContadorPrograma(int contadorPrograma) {
        this.contadorPrograma = contadorPrograma;
    }

    /**
     * Devuelve el contenido del registro de dirección de memoria.
     * @return registro de dirección de memoria
     */
    public int getRegistroDireccionMemoria() {
        return registroDireccionMemoria;
    }

    /**
     * Establece el contenido del registro de dirección de memoria.
     * @param registroDireccionMemoria nuevo valor del registro
     */
    public void setRegistroDireccionMemoria(int registroDireccionMemoria) {
        this.registroDireccionMemoria = registroDireccionMemoria;
    }

    /**
     * Devuelve el número total de instrucciones asignado al proceso.
     * @return instrucciones totales
     */
    public int getInstruccionesTotales() {
        return instruccionesTotales;
    }

    /**
     * Establece el número total de instrucciones y actualiza las restantes.
     * @param instruccionesTotales nuevo total de instrucciones
     */
    public void setInstruccionesTotales(int instruccionesTotales) {
        this.instruccionesTotales = instruccionesTotales;
        this.instruccionesRestantes = instruccionesTotales;
    }

    /**
     * Devuelve el número de instrucciones restantes.
     * @return instrucciones restantes
     */
    public int getInstruccionesRestantes() {
        return instruccionesRestantes;
    }
    
    // Permite ajustar instrucciones restantes (útil para tareas aleatorias en ejecución)
public void setInstruccionesRestantes(int instruccionesRestantes) {
    this.instruccionesRestantes = Math.max(0, instruccionesRestantes);
    // Si alguien lo aumenta por encima del total, actualizo total para consistencia visual
    if (this.instruccionesRestantes > this.instruccionesTotales) {
        this.instruccionesTotales = this.instruccionesRestantes;
    }
}

    /**
     * Decrementa en uno el contador de instrucciones restantes.
     * Si llega a cero, el proceso habrá terminado su ejecución.
     */
    public void decrementarInstruccion() {
        if (instruccionesRestantes > 0) {
            instruccionesRestantes--;
        }
    }

    /**
     * Devuelve el tiempo restante de ráfaga de E/S.
     * @return ráfaga de E/S restante
     */
    public int getRafagaESRestante() {
        return rafagaESRestante;
    }

    /**
     * Establece el tiempo restante de ráfaga de E/S.
     * @param rafagaESRestante nuevo tiempo de ráfaga de E/S restante
     */
    public void setRafagaESRestante(int rafagaESRestante) {
        this.rafagaESRestante = rafagaESRestante;
    }

    /**
     * Devuelve el periodo de reactivación del proceso.
     * @return periodo (puede ser null si no es periódico)
     */
    public Integer getPeriodo() {
        return periodo;
    }

    /**
     * Establece el periodo de reactivación del proceso.
     * @param periodo nuevo periodo (puede ser null)
     */
    public void setPeriodo(Integer periodo) {
        this.periodo = periodo;
    }

    /**
     * Calcula el tiempo restante hasta el límite respecto al ciclo de reloj actual.
     * Este método debe ser llamado con el ciclo de reloj global como argumento.
     *
     * @param tickActual ciclo de reloj actual
     * @return tiempo restante en ciclos; puede ser negativo si el límite ya pasó
     */
    public long tiempoRestante(long tickActual) {
        return tiempoLimite - tickActual;
    }

    @Override
    public String toString() {
        return String.format(
                "BCP{id=%d, nombre='%s', estado=%s, prioridad=%d, tiempoLimite=%d, CP=%d, RDM=%d, restantes=%d}",
                id, nombre, estado, prioridad, tiempoLimite, contadorPrograma, registroDireccionMemoria, instruccionesRestantes);
    }
}