/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */

import javax.swing.*;
import java.nio.file.Path;

public class MainGUI {

    public static void main(String[] args) {
        // Creamos el simulador (tick arranca en 0)
        NucleoSimulador sim = new NucleoSimulador(250);

        // ===== Selector inicial: RANDOM vs CSV =====
        String[] opciones = {"Iniciar con RANDOM", "Cargar desde CSV"};
        int opcion = JOptionPane.showOptionDialog(
                null,
                "¿Cómo quieres inicializar el simulador?",
                "Inicialización",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        boolean cargado = false;

        if (opcion == 1) { // CSV
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Selecciona el archivo CSV de procesos");

            int r = fc.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) {
                try {
                    Path ruta = fc.getSelectedFile().toPath();

                    // Limpio colas antes de cargar
                    sim.limpiarColas();

                    // Cargo procesos desde CSV -> cola NUEVO
                    ListaEnlazada<BCP> lista = CargadorProcesosCSV.cargar(ruta);
                    int n = sim.cargarDesdeLista(lista);

                    if (n > 0) {
                        cargado = true;
                        JOptionPane.showMessageDialog(null, "Cargados " + n + " procesos desde CSV.");
                    } else {
                        JOptionPane.showMessageDialog(null, "El CSV no cargó procesos. Se iniciará con RANDOM.");
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error cargando CSV:\n" + ex.getMessage() + "\n\nSe iniciará con RANDOM.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Cancelaste el CSV. Se iniciará con RANDOM.");
            }
        }

        // Si no cargó CSV, cae al modo random como antes
        if (!cargado) {
            sim.cargarInicial(15);
        }

        // Arranque de hilos
        new Thread(sim).start();
        new Thread(new GeneradorInterrupciones(sim)).start();

        // GUI
        SwingUtilities.invokeLater(() -> {
            VentanaSimulador v = new VentanaSimulador(sim);
            sim.setListener(snap -> SwingUtilities.invokeLater(() -> v.actualizar(snap)));
            v.setVisible(true);
        });
    }
}