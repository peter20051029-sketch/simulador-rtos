/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */
public class MainPrueba {

    public static void main(String[] args) {
        long tick = 0; // inicio

        GeneradorProcesos g = new GeneradorProcesos();
        g.generar(5, tick);

        System.out.println("=== Generados ===");
        g.imprimir();

        System.out.println("=== Primero ===");
        System.out.println(g.obtener().verPrimero());
    }
}

