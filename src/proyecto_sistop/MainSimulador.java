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
        NucleoSimulador sim = new NucleoSimulador(250);
        sim.cargarInicial(10);

        new Thread(sim).start();
        new Thread(new GeneradorInterrupciones(sim)).start();

        sim.iniciar();

        Thread.sleep(3000);
        sim.setPolitica(new PlanificadorEDF());

        Thread.sleep(4000);
        sim.setPolitica(new PlanificadorSRT());

        Thread.sleep(4000);
        sim.setPolitica(new PlanificadorPrioridad());

        Thread.sleep(4000);
        sim.setPolitica(new PlanificadorRR(3));
    }
}

