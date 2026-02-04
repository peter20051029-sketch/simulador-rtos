package proyecto_sistop;

/**
 * Enumeración de los posibles estados de un proceso dentro del sistema.
 * Incluye tanto los estados básicos como los estados suspendidos según
 * el modelo de transición especificado en el enunciado del proyecto.
 */
public enum EstadoProceso {
    /** El proceso ha sido creado pero aún no está listo para ejecutarse. */
    NUEVO,
    /** El proceso está listo para ejecutarse en la CPU. */
    LISTO,
    /** El proceso está siendo ejecutado por la CPU. */
    EJECUCION,
    /** El proceso está bloqueado, generalmente esperando una operación de E/S. */
    BLOQUEADO,
    /** El proceso ha finalizado su ejecución. */
    TERMINADO,
    /** El proceso listo ha sido suspendido y movido a memoria secundaria. */
    LISTO_SUSPENDIDO,
    /** El proceso bloqueado ha sido suspendido y movido a memoria secundaria. */
    BLOQUEADO_SUSPENDIDO;
}