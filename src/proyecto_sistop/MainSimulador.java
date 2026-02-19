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

    public static void main(String[] args) throws InterruptedException {

        NucleoSimulador sim = new NucleoSimulador(300);
        sim.cargarInicial(6);

        Thread reloj = new Thread(sim);
        reloj.start();

        Thread hiloInterrupciones = new Thread(new GeneradorInterrupciones(sim));
        hiloInterrupciones.start();

        sim.iniciar();

        Thread.sleep(3000);
        sim.setPolitica(new PlanificadorRR(3));
    }
}
