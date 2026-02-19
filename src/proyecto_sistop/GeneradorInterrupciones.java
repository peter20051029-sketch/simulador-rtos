/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */
import java.util.Random;

public class GeneradorInterrupciones implements Runnable {

    private final Random random;
    private final NucleoSimulador nucleo;

    public GeneradorInterrupciones(NucleoSimulador nucleo) {
        this.random = new Random();
        this.nucleo = nucleo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                int esperaMs = random.nextInt(2500) + 1500; // 1.5s..4s
                Thread.sleep(esperaMs);

                nucleo.dispararInterrupcion();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

