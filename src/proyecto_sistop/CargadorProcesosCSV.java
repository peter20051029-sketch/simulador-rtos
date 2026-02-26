/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_sistop;

/**
 *
 * @author Peter
 */
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CargadorProcesosCSV {

    /**
     * CSV esperado (con o sin encabezado):
     * nombre,prioridad,deadline,instrucciones,rafagaES,periodo
     *
     * Ejemplo:
     * P_A,3,120,80,10,
     * P_B,1,90,60,0,
     * P_C,5,200,140,8,50
     *
     * - periodo vacío => null
     * - deadline se interpreta como ABSOLUTO (como tick arranca en 0, es equivalente a relativo)
     */
    public static ListaEnlazada<BCP> cargar(Path ruta) throws Exception {
        List<String> lineas = Files.readAllLines(ruta);
        ListaEnlazada<BCP> salida = new ListaEnlazada<>();

        for (String linea : lineas) {
            if (linea == null) continue;
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            // Ignorar comentarios
            if (linea.startsWith("#")) continue;

            // Detectar separador (coma o punto y coma)
            String sep = linea.contains(";") ? ";" : ",";

            // Saltar encabezado típico
            String lower = linea.toLowerCase();
            if (lower.contains("nombre") && lower.contains("prioridad") && lower.contains("deadline")) {
                continue;
            }

            String[] p = linea.split(sep);
            if (p.length < 5) {
                // Línea inválida
                continue;
            }

            String nombre = limpiar(p[0]);
            int prioridad = Integer.parseInt(limpiar(p[1]));
            long deadline = Long.parseLong(limpiar(p[2]));
            int instrucciones = Integer.parseInt(limpiar(p[3]));
            int rafagaES = Integer.parseInt(limpiar(p[4]));

            Integer periodo = null;
            if (p.length >= 6) {
                String per = limpiar(p[5]);
                if (!per.isEmpty()) {
                    periodo = Integer.parseInt(per);
                }
            }

            // Como el tick al inicio es 0, deadline absoluto funciona perfecto
            long tiempoLimite = deadline;

            BCP bcp = new BCP(nombre, prioridad, tiempoLimite, instrucciones, rafagaES, periodo);

            salida.encolar(bcp);
        }

        return salida;
    }

    private static String limpiar(String s) {
        if (s == null) return "";
        s = s.trim();
        // quitar comillas simples o dobles si vienen
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }
}
