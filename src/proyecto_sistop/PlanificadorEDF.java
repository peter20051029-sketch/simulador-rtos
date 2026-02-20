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

public class PlanificadorEDF implements PoliticaPlanificacion {

    public static final Comparator<BCP> COMP =
            (a, b) -> Long.compare(a.getTiempoLimite(), b.getTiempoLimite());

    @Override
    public BCP elegirSiguiente(ListaEnlazada<BCP> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public String nombre() {
        return "EDF";
    }
}

