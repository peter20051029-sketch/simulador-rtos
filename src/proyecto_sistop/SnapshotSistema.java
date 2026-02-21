/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */
public class SnapshotSistema {
    public long tick;
    public String politica;

    public String ejecutando;

    public int nuevo, listo, emergencia, bloqueado, terminado, listoSusp, bloqSusp;

    public double cpuUtil;
    public double tasaExito;
    public int deadlinesFallados;
    public int terminadosTotales;
    public int interrupciones;

    public String log;

    // Indicador de modo (SO/Kernel vs Usuario)
    public boolean modoKernel;

    // Textos de colas con PCB por proceso
    public String txtNuevo;
    public String txtListo;
    public String txtBloqueado;
    public String txtTerminado;
    public String txtListoSusp;
    public String txtBloqSusp;
    public String txtEmergencia;

    // Promedios generales
    public double throughput;       // terminados/tick
    public double esperaPromedio;   // espera promedio en READY
}