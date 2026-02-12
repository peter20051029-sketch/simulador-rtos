/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

public interface PoliticaPlanificacion {
    BCP elegirSiguiente(ListaEnlazada<BCP> colaListos);
    String nombre();
}

