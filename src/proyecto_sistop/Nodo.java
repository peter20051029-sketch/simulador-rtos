package proyecto_sistop;

/**
 * Nodo genérico para la implementación de estructuras enlazadas.  Cada nodo
 * referencia a un elemento de datos y al siguiente nodo en la estructura.
 *
 * @param <T> tipo de dato almacenado en el nodo
 */
public class Nodo<T> {
    /** El dato almacenado en este nodo. */
    public T dato;
    /** Referencia al siguiente nodo en la lista; puede ser null para el último nodo. */
    public Nodo<T> siguiente;

    /**
     * Construye un nuevo nodo que almacena el valor indicado.
     *
     * @param dato valor a almacenar
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }
}