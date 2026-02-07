/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

public class MainSimulador {

    public static void main(String[] args) {
        NucleoSimulador simulador = new NucleoSimulador(500);

        simulador.cargarInicial(5);

        Thread hiloReloj = new Thread(simulador);
        hiloReloj.start();

        simulador.iniciar();
    }
}

