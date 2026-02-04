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

public class GeneradorProcesos {

    private final Random random;
    private final ListaEnlazada<BCP> procesos; // buffer

    public GeneradorProcesos() {
        this.random = new Random();
        this.procesos = new ListaEnlazada<>();
    }

    public void generar(int cantidad, long tickActual) {
        for (int i = 1; i <= cantidad; i++) {

            String nombre = "P" + i;
            int prioridad = random.nextInt(10) + 1;         // 1..10
            int instrucciones = random.nextInt(191) + 10;   // 10..200
            int rafagaES = random.nextInt(16);              // 0..15

            Integer periodo = null; // aperiódico
            if (random.nextInt(100) < 20) {                 // 20%
                periodo = random.nextInt(61) + 20;          // 20..80
            }

            long tiempoLimite = tickActual + (random.nextInt(171) + 30); // 30..200

            BCP p = new BCP(nombre, prioridad, tiempoLimite, instrucciones, rafagaES, periodo);
            procesos.encolar(p);
        }
    }

    public ListaEnlazada<BCP> obtener() {
        return procesos;
    }

    public void imprimir() {
        procesos.paraCada(System.out::println);
    }
}
