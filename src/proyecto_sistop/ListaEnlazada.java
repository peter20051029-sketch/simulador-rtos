package proyecto_sistop;

import java.util.Comparator;

/**
 * Implementación simple de una lista enlazada y cola personalizada.  Esta clase
 * evita el uso de las colecciones de java.util que están prohibidas por el
 * enunciado del proyecto y provee operaciones básicas para manipular
 * estructuras tipo cola (FIFO) así como inserciones ordenadas.
 *
 * @param <T> tipo de dato almacenado en la lista
 */
public class ListaEnlazada<T> {
    /** Nodo inicial de la lista (cabeza). */
    private Nodo<T> cabeza;
    /** Nodo final de la lista (cola). */
    private Nodo<T> cola;
    /** Número de elementos almacenados. */
    private int tamano;

    /**
     * Crea una lista vacía.
     */
    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    /**
     * Devuelve true si la lista está vacía.
     * @return true si no hay elementos
     */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /**
     * Devuelve el número de elementos en la lista.
     * @return número de elementos
     */
    public int tamano() {
        return tamano;
    }

    /**
     * Inserta un elemento al final de la lista (FIFO).  Se emplea para
     * algoritmos como FCFS y Round Robin donde la cola se comporta de
     * forma FIFO simple.
     * @param dato elemento a insertar
     */
    public void encolar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (estaVacia()) {
            cabeza = cola = nuevoNodo;
        } else {
            cola.siguiente = nuevoNodo;
            cola = nuevoNodo;
        }
        tamano++;
    }

    /**
     * Remueve y devuelve el elemento al inicio de la lista.
     * @return elemento removido o null si la lista está vacía
     */
    public T desencolar() {
        if (estaVacia()) return null;
        T valor = cabeza.dato;
        cabeza = cabeza.siguiente;
        // Si la cabeza se vuelve null, también actualizar la cola
        if (cabeza == null) {
            cola = null;
        }
        tamano--;
        return valor;
    }

    /**
     * Inserta un elemento en la lista en orden ascendente según un comparador
     * provisto.  Esta operación es útil para algoritmos como SRT o EDF donde
     * la lista debe mantenerse ordenada por alguna métrica (por ejemplo, tiempo
     * restante o deadline). Si el comparador indica que el nuevo elemento es
     * "menor" que un nodo existente, se insertará antes de ese nodo.
     *
     * @param dato elemento a insertar
     * @param comparador comparador que define el orden de los elementos
     */
    public void insertarOrdenado(T dato, Comparator<T> comparador) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (estaVacia()) {
            cabeza = cola = nuevoNodo;
            tamano++;
            return;
        }
        // Si el nuevo nodo debe ir al principio
        if (comparador.compare(dato, cabeza.dato) < 0) {
            nuevoNodo.siguiente = cabeza;
            cabeza = nuevoNodo;
            tamano++;
            return;
        }
        // Insertar en la mitad o final
        Nodo<T> actual = cabeza;
        while (actual.siguiente != null && comparador.compare(dato, actual.siguiente.dato) >= 0) {
            actual = actual.siguiente;
        }
        nuevoNodo.siguiente = actual.siguiente;
        actual.siguiente = nuevoNodo;
        // Si se insertó al final, actualizar la cola
        if (actual == cola) {
            cola = nuevoNodo;
        }
        tamano++;
    }

    /**
     * Devuelve (sin quitar) el elemento al inicio de la lista, o null si está vacía.
     * @return elemento en la cabeza o null
     */
    public T verPrimero() {
        return estaVacia() ? null : cabeza.dato;
    }

    /**
     * Permite recorrer la lista con un consumidor.
     * @param accion acción a aplicar a cada elemento
     */
    public void paraCada(java.util.function.Consumer<T> accion) {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            accion.accept(actual.dato);
            actual = actual.siguiente;
        }
    }
}