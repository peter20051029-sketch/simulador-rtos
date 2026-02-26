/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

import java.util.Comparator;

public class PlanificadorPrioridad implements PoliticaPlanificacion {

    public static final Comparator<BCP> COMP =
            (a, b) -> Integer.compare(a.getPrioridad(), b.getPrioridad()); // 1 = más alta

    @Override
    public BCP elegirSiguiente(ListaEnlazada<BCP> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public String nombre() {
        return "PRIORIDAD";
    }
}

