/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

public class PlanificadorRR implements PoliticaPlanificacion {

    private final int quantum;

    public PlanificadorRR(int quantum) {
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public BCP elegirSiguiente(ListaEnlazada<BCP> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public String nombre() {
        return "RR(q=" + quantum + ")";
    }
}
