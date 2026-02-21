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
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class VentanaSimulador extends JFrame {

    private static final Color FONDO = new Color(10, 10, 16);
    private static final Color PANEL = new Color(18, 18, 28);
    private static final Color TEXTO = new Color(230, 240, 255);
    private static final Color BORDE = new Color(140, 90, 255);
    private static final Color BORDE2 = new Color(80, 220, 255);

    private final JButton btnCambiarVista = new JButton("VER GRAFICO");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelVistas = new JPanel(cardLayout);
    private boolean mostrandoGrafico = false;

    private final JLabel lblClock = new JLabel("MISSION CLOCK: Cycle 0");
    private final JProgressBar barraMemoria = new JProgressBar(0, 100);

    private final JTextArea areaReadyRam = new JTextArea();
    private final JTextArea areaBloqRam = new JTextArea();
    private final JTextArea areaReadySusp = new JTextArea();
    private final JTextArea areaBloqSusp = new JTextArea();

    // ===== NUEVO: mostrar culminados y cola de emergencia =====
    private final JTextArea areaTerminado = new JTextArea();
    private final JTextArea areaEmergencia = new JTextArea();

    private final JTextArea areaLog = new JTextArea();

    private final JLabel lblRunning = new JLabel("RUNNING: Ninguno");
    private final JLabel lblMetrics = new JLabel("CPU Util: 0% | Éxito: 0% | Fallos: 0 | Term: 0 | Int: 0");
    private final JLabel lblModo = new JLabel("MODO: KERNEL");

    private final JButton btnStart = new JButton("START");
    private final JButton btnStop = new JButton("STOP");
    private final JButton btnGen20 = new JButton("GENERAR 20");
    private final JButton btnReset = new JButton("RESET");
    private final JButton btnGuardarLog = new JButton("GUARDAR LOG");

    private final JButton btnNuevoCPU = new JButton("+ CPU");
    private final JButton btnNuevoIO = new JButton("+ I/O");
    private final JButton btnNuevoMixto = new JButton("+ MIXTO");

    private final JButton btnEmergencia = new JButton("EMERGENCY INTERRUPTION");

    private final JComboBox<String> comboPolitica = new JComboBox<>(new String[]{"FCFS", "RR", "SRT", "PRIORIDAD", "EDF"});
    private final JSpinner spinQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
    private final JSpinner spinCiclo = new JSpinner(new SpinnerNumberModel(250, 50, 2000, 50));

    private final PanelGraficoCPU graficoCPU = new PanelGraficoCPU();

    private final NucleoSimulador sim;

    public VentanaSimulador(NucleoSimulador sim) {
        super("UNIMET-Sat RTOS Simulator - Mission Control");
        this.sim = sim;

        estilizarLog(areaLog);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(FONDO);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        // ====== TOP ======
        JPanel top = panelNeon("UNIMET-Sat RTOS Simulator - Memory Management & Swap");
        top.setLayout(new BorderLayout(12, 12));

        lblClock.setForeground(BORDE2);
        lblClock.setFont(new Font("Consolas", Font.BOLD, 20));

        JPanel rightClock = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightClock.setOpaque(false);
        rightClock.add(lblClock);

        JPanel memPanel = new JPanel(new BorderLayout(6, 6));
        memPanel.setOpaque(false);
        JLabel memLabel = new JLabel("Main Memory (RAM) - Memory Usage");
        memLabel.setForeground(TEXTO);

        barraMemoria.setValue(0);
        barraMemoria.setStringPainted(true);
        barraMemoria.setForeground(BORDE2);
        barraMemoria.setBackground(new Color(40, 40, 60));
        barraMemoria.setBorder(BorderFactory.createLineBorder(BORDE));

        memPanel.add(memLabel, BorderLayout.NORTH);
        memPanel.add(barraMemoria, BorderLayout.CENTER);

        top.add(memPanel, BorderLayout.CENTER);
        top.add(rightClock, BorderLayout.EAST);

        // ====== COLAS (ahora 3x2 para incluir TERMINATED y EMERGENCY) ======
        JPanel centroColas = new JPanel(new GridLayout(3, 2, 12, 12));
        centroColas.setBackground(FONDO);

        centroColas.add(panelCola("Ready Queue (RAM)", areaReadyRam));
        centroColas.add(panelCola("Blocked Queue (RAM)", areaBloqRam));
        centroColas.add(panelCola("Ready-Suspended", areaReadySusp));
        centroColas.add(panelCola("Blocked-Suspended", areaBloqSusp));
        centroColas.add(panelCola("Terminated", areaTerminado));
        centroColas.add(panelCola("Emergency Queue", areaEmergencia));

        // ====== GRAFICO ======
        JPanel centroGrafico = new JPanel(new BorderLayout(12, 12));
        centroGrafico.setBackground(FONDO);

        JPanel panelGrafico = panelNeon("CPU Util (%) vs tiempo");
        panelGrafico.setLayout(new BorderLayout(6, 6));
        panelGrafico.add(graficoCPU, BorderLayout.CENTER);

        centroGrafico.add(panelGrafico, BorderLayout.CENTER);

        // ====== CARDS ======
        panelVistas.add(centroColas, "COLAS");
        panelVistas.add(centroGrafico, "GRAFICO");
        cardLayout.show(panelVistas, "COLAS");

        // ====== LOG fijo ======
        JPanel logPanel = panelCola("Event Log", areaLog);
        logPanel.setPreferredSize(new Dimension(380, 0));

        JPanel centerWrap = new JPanel(new BorderLayout(12, 12));
        centerWrap.setBackground(FONDO);
        centerWrap.add(panelVistas, BorderLayout.CENTER);
        centerWrap.add(logPanel, BorderLayout.EAST);

        // ====== BOTTOM ======
        JPanel bottom = panelNeon("UNIMET-Sat RTOS Simulator - Mission Control");
        bottom.setLayout(new BorderLayout(12, 12));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setOpaque(true);
        info.setBackground(new Color(12, 12, 20));
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE2, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        Color FONDO_INFO = new Color(12, 12, 20);

        lblRunning.setOpaque(true);
        lblRunning.setBackground(FONDO_INFO);
        lblRunning.setForeground(new Color(230, 240, 255));
        lblRunning.setFont(new Font("Consolas", Font.BOLD, 16));
        lblRunning.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblMetrics.setOpaque(true);
        lblMetrics.setBackground(FONDO_INFO);
        lblMetrics.setForeground(new Color(200, 220, 255));
        lblMetrics.setFont(new Font("Consolas", Font.PLAIN, 14));
        lblMetrics.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblModo.setOpaque(true);
        lblModo.setBackground(FONDO_INFO);
        lblModo.setForeground(BORDE2);
        lblModo.setFont(new Font("Consolas", Font.BOLD, 14));
        lblModo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        info.add(lblRunning);
        info.add(lblMetrics);
        info.add(lblModo);

        btnEmergencia.setFont(new Font("Arial", Font.BOLD, 14));
        btnEmergencia.setForeground(Color.WHITE);
        btnEmergencia.setBackground(new Color(190, 30, 30));
        btnEmergencia.setFocusPainted(false);
        btnEmergencia.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 80, 80), 2),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        btnEmergencia.addActionListener(e -> sim.dispararInterrupcion());

        JPanel middleBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        middleBottom.setOpaque(false);
        middleBottom.add(btnEmergencia);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.setOpaque(false);

        estiloBoton(btnCambiarVista, new Color(40, 140, 140));
        estiloBoton(btnStart, new Color(20, 120, 60));
        estiloBoton(btnStop, new Color(120, 60, 20));
        estiloBoton(btnGen20, new Color(60, 60, 120));
        estiloBoton(btnReset, new Color(120, 20, 90));
        estiloBoton(btnGuardarLog, new Color(40, 90, 140));

        estiloBoton(btnNuevoCPU, new Color(80, 100, 200));
        estiloBoton(btnNuevoIO, new Color(200, 140, 60));
        estiloBoton(btnNuevoMixto, new Color(120, 180, 120));

        btnCambiarVista.addActionListener(e -> {
            mostrandoGrafico = !mostrandoGrafico;
            if (mostrandoGrafico) {
                cardLayout.show(panelVistas, "GRAFICO");
                btnCambiarVista.setText("VER COLAS");
            } else {
                cardLayout.show(panelVistas, "COLAS");
                btnCambiarVista.setText("VER GRAFICO");
            }
        });

        btnStart.addActionListener(e -> sim.iniciar());
        btnStop.addActionListener(e -> sim.detener());
        btnGen20.addActionListener(e -> sim.cargarInicial(20));

        btnNuevoCPU.addActionListener(e -> sim.agregarProcesoAleatorioCPU());
        btnNuevoIO.addActionListener(e -> sim.agregarProcesoAleatorioIO());
        btnNuevoMixto.addActionListener(e -> sim.agregarProcesoAleatorioMixto());

        btnReset.addActionListener(e -> {
            btnReset.setEnabled(false);
            btnStart.setEnabled(false);
            btnStop.setEnabled(false);
            btnGen20.setEnabled(false);
            btnEmergencia.setEnabled(false);

            new Thread(() -> {
                sim.resetearSistema(15);
                sim.iniciar();

                SwingUtilities.invokeLater(() -> {
                    btnReset.setEnabled(true);
                    btnStart.setEnabled(true);
                    btnStop.setEnabled(true);
                    btnGen20.setEnabled(true);
                    btnEmergencia.setEnabled(true);
                });
            }).start();
        });

        btnGuardarLog.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar Event Log");
            fc.setSelectedFile(new java.io.File("event_log.txt"));

            int r = fc.showSaveDialog(this);
            if (r == JFileChooser.APPROVE_OPTION) {
                try {
                    String texto = sim.obtenerLogTexto();
                    Path ruta = fc.getSelectedFile().toPath();
                    Files.writeString(ruta, texto);
                    JOptionPane.showMessageDialog(this, "Log guardado en:\n" + ruta);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error guardando log:\n" + ex.getMessage());
                }
            }
        });

        comboPolitica.addActionListener(e -> aplicarPolitica());
        spinCiclo.addChangeListener(e -> sim.setDuracionCicloMs((Integer) spinCiclo.getValue()));

        JLabel lPol = etiqueta("Política:");
        JLabel lQ = etiqueta("Quantum:");
        JLabel lC = etiqueta("Ciclo(ms):");

        controles.add(btnCambiarVista);
        controles.add(btnStart);
        controles.add(btnStop);
        controles.add(btnGen20);
        controles.add(btnReset);
        controles.add(btnGuardarLog);

        controles.add(new JLabel("  Nuevos: "));
        controles.add(btnNuevoCPU);
        controles.add(btnNuevoIO);
        controles.add(btnNuevoMixto);

        controles.add(Box.createHorizontalStrut(15));
        controles.add(lPol);
        controles.add(comboPolitica);
        controles.add(Box.createHorizontalStrut(10));
        controles.add(lQ);
        controles.add(spinQuantum);
        controles.add(Box.createHorizontalStrut(10));
        controles.add(lC);
        controles.add(spinCiclo);

        bottom.add(info, BorderLayout.NORTH);
        bottom.add(middleBottom, BorderLayout.CENTER);
        bottom.add(controles, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private String ultimasLineas(String texto, int maxLineas) {
        if (texto == null || texto.isEmpty()) return "";
        String[] lineas = texto.split("\n");
        int ini = Math.max(0, lineas.length - maxLineas);
        StringBuilder sb = new StringBuilder();
        for (int i = ini; i < lineas.length; i++) sb.append(lineas[i]).append("\n");
        return sb.toString();
    }

    private JLabel etiqueta(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(TEXTO);
        return l;
    }

    private void estilizarLog(JTextArea area) {
        area.setEditable(false);
        area.setBackground(new Color(12, 12, 20));
        area.setForeground(new Color(220, 235, 255));
        area.setCaretColor(new Color(220, 235, 255));
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void estiloBoton(JButton b, Color c) {
        b.setForeground(Color.WHITE);
        b.setBackground(c);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(BORDE2));
    }

    private JPanel panelNeon(String titulo) {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(bordeNeon(titulo));
        return p;
    }

    private JPanel panelCola(String titulo, JTextArea area) {
        JPanel p = panelNeon(titulo);
        p.setLayout(new BorderLayout(6, 6));
        estiloArea(area);

        JScrollPane sp = new JScrollPane(area);
        sp.getViewport().setBackground(new Color(12, 12, 20));
        sp.setBackground(new Color(12, 12, 20));
        sp.setBorder(BorderFactory.createLineBorder(BORDE2, 1));

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void estiloArea(JTextArea area) {
        area.setEditable(false);
        area.setBackground(new Color(12, 12, 20));
        area.setForeground(new Color(220, 235, 255));
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private Border bordeNeon(String titulo) {
        Border outer = BorderFactory.createLineBorder(BORDE, 2);
        Border inner = BorderFactory.createLineBorder(BORDE2, 1);
        Border title = BorderFactory.createTitledBorder(outer, " " + titulo + " ",
                0, 0, new Font("Consolas", Font.BOLD, 13), TEXTO);
        return BorderFactory.createCompoundBorder(title,
                BorderFactory.createCompoundBorder(inner, BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    }

    private void aplicarPolitica() {
        String p = (String) comboPolitica.getSelectedItem();
        if (p == null) return;

        switch (p) {
            case "FCFS" -> sim.setPolitica(new PlanificadorFCFS());
            case "RR" -> sim.setPolitica(new PlanificadorRR((Integer) spinQuantum.getValue()));
            case "SRT" -> sim.setPolitica(new PlanificadorSRT());
            case "PRIORIDAD" -> sim.setPolitica(new PlanificadorPrioridad());
            case "EDF" -> sim.setPolitica(new PlanificadorEDF());
        }
    }

    public void actualizar(SnapshotSistema s) {
        lblClock.setText("MISSION CLOCK: Cycle " + s.tick);
        lblModo.setText(s.modoKernel ? "MODO: KERNEL" : "MODO: USUARIO");

        lblMetrics.setText(String.format(
                "CPU Util: %.2f%% | Éxito: %.2f%% | Fallos: %d | Term: %d | Int: %d | Th: %.4f | EspProm: %.2f",
                s.cpuUtil, s.tasaExito, s.deadlinesFallados, s.terminadosTotales, s.interrupciones,
                s.throughput, s.esperaPromedio
        ));

        lblRunning.setText("RUNNING: " + s.ejecutando + "   |   Política: " + s.politica);

        int enRam = s.listo + s.bloqueado + (s.ejecutando.equals("Ninguno") ? 0 : 1);
        int porcentaje = (int) Math.min(100, (enRam * 100.0 / 5.0));
        barraMemoria.setValue(porcentaje);

        if (s.txtListo != null) areaReadyRam.setText(s.txtListo);
        if (s.txtBloqueado != null) areaBloqRam.setText(s.txtBloqueado);
        if (s.txtListoSusp != null) areaReadySusp.setText(s.txtListoSusp);
        if (s.txtBloqSusp != null) areaBloqSusp.setText(s.txtBloqSusp);

        // NUEVO: mostrar culminados y emergencia
        if (s.txtTerminado != null) areaTerminado.setText(s.txtTerminado);
        if (s.txtEmergencia != null) areaEmergencia.setText(s.txtEmergencia);

        areaLog.setText(ultimasLineas(s.log, 200));
        areaLog.setCaretPosition(areaLog.getDocument().getLength());

        graficoCPU.addValor(s.cpuUtil);
    }

    static class PanelGraficoCPU extends JPanel {
        private final int MAX = 250;
        private final double[] y = new double[MAX];
        private int n = 0;

        public PanelGraficoCPU() {
            setPreferredSize(new Dimension(380, 160));
            setBackground(new Color(12, 12, 20));
            setBorder(BorderFactory.createLineBorder(new Color(80, 220, 255), 1));
        }

        public void addValor(double util) {
            if (n < MAX) {
                y[n++] = util;
            } else {
                for (int i = 1; i < MAX; i++) y[i - 1] = y[i];
                y[MAX - 1] = util;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth() - 20;
            int h = getHeight() - 30;
            int x0 = 10, y0 = 20;

            g.setColor(new Color(230, 240, 255));
            g.drawString("CPU Util (%) vs tiempo", 10, 14);

            g.setColor(new Color(80, 220, 255));
            g.drawRect(x0, y0, w, h);

            if (n < 2) return;

            g.setColor(new Color(140, 90, 255));
            int prevX = x0;
            int prevY = y0 + h - (int) (h * (y[0] / 100.0));

            for (int i = 1; i < n; i++) {
                int x = x0 + (int) ((i / (double) (MAX - 1)) * w);
                int yy = y0 + h - (int) (h * (y[i] / 100.0));
                g.drawLine(prevX, prevY, x, yy);
                prevX = x;
                prevY = yy;
            }
        }
    }
}