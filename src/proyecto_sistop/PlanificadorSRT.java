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

public class PlanificadorSRT implements PoliticaPlanificacion {

    public static final Comparator<BCP> COMP =
            (a, b) -> Integer.compare(a.getInstruccionesRestantes(), b.getInstruccionesRestantes());

    @Override
    public BCP elegirSiguiente(ListaEnlazada<BCP> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public String nombre() {
        return "SRT";
    }
}

