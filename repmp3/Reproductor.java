package repmp3;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import repmp3.lexer.Lexer;
import repmp3.node.Start;
import repmp3.parser.Parser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.PushbackReader;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class Reproductor implements BasicPlayerListener {

    private BasicPlayer player;

    private JFrame frame;
    private JLabel lblTitulo;
    private JLabel lblArtista;
    private JLabel lblAlbum;
    private JLabel lblLetraActual;

    private JProgressBar progressBar;
    private long totalDuracionMicrosegundos = -1;

    // Variables de estado
    private int estadoReproductor = 0; // 0: Detenido, 1: Reproduciendo, 2: Pausado
    private long tiempoActualMilisegundos = 0;

    private List<LineaLetra> repositorioLetras;
    private String rutaArchivoAudio;
    private Reminder timerLetras;

    private Start arbol;
    private boolean arbolMostrado = false;

    Reproductor(){
        player = new BasicPlayer();
        player.addBasicPlayerListener(this);
        repositorioLetras = new ArrayList<>();
        configurarGUI();
    }

    public void Play() throws Exception {
        player.play();
    }

    public void AbrirFichero(String ruta) throws Exception {
        player.open(new File(ruta));
    }

    public void Pausa() throws Exception {
        player.pause();
    }

    public void Continuar() throws Exception {
        player.resume();
    }

    public void Stop() throws Exception {
        player.stop();
    }

    private void configurarGUI() {
        frame = new JFrame("Reproductor");
        frame.setSize(650, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(30, 30, 30));
        frame.setLayout(new BorderLayout());

        // PANEL SUPERIOR
        JPanel panelInfo = new JPanel(new GridLayout(3, 1));
        panelInfo.setOpaque(false);
        lblTitulo = new JLabel("Canción: (Ninguna)", SwingConstants.CENTER);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));

        lblArtista = new JLabel("Artista: (Ninguno)", SwingConstants.CENTER);
        lblArtista.setForeground(Color.LIGHT_GRAY);
        lblArtista.setFont(new Font("Arial", Font.PLAIN, 18));

        lblAlbum = new JLabel("Álbum: (Ninguno)", SwingConstants.CENTER);
        lblAlbum.setForeground(Color.GRAY);
        lblAlbum.setFont(new Font("Arial", Font.ITALIC, 14));

        panelInfo.add(lblTitulo);
        panelInfo.add(lblArtista);
        panelInfo.add(lblAlbum);
        panelInfo.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        frame.add(panelInfo, BorderLayout.NORTH);

        // CENTRO
        lblLetraActual = new JLabel("Seleccione una canción para comenzar", SwingConstants.CENTER);
        lblLetraActual.setForeground(new Color(50, 205, 50));
        lblLetraActual.setFont(new Font("Arial", Font.BOLD, 28));
        frame.add(lblLetraActual, BorderLayout.CENTER);

        // PANEL INFERIOR
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setOpaque(false);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("00:00 / 00:00");
        progressBar.setBackground(Color.DARK_GRAY);
        progressBar.setForeground(new Color(50, 205, 50));
        progressBar.setFont(new Font("Arial", Font.BOLD, 12));
        panelInferior.add(progressBar, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panelBotones.setOpaque(false);

        JButton btnCargar = crearBoton("Cargar MP3");
        JButton btnStop = crearBoton("■ Stop");
        JButton btnPlay = crearBoton("► Play");
        JButton btnPause = crearBoton("II Pause");

        btnCargar.addActionListener(e -> cargarArchivos());

        // --- LÓGICA DE PLAY / RESUME ACTUALIZADA ---
        btnPlay.addActionListener(e -> {
            try {
                if (rutaArchivoAudio != null) {
                    if (estadoReproductor == 2) {
                        // Si estaba pausado, llamamos a Continuar()
                        Continuar();
                        estadoReproductor = 1;
                        timerLetras = new Reminder(repositorioLetras, tiempoActualMilisegundos);

                    } else if (estadoReproductor == 0) {
                        // Si estaba detenido, iniciamos de cero
                        if (arbol != null && !arbolMostrado) {
                            arbol.apply(new ASTDisplay());
                            arbolMostrado = true;
                        }

                        lblLetraActual.setText("♪ ... (Instrumental) ... ♪");

                        Play();
                        estadoReproductor = 1;

                        if (timerLetras != null) timerLetras.cancelar();
                        timerLetras = new Reminder(repositorioLetras, 0);
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error al reproducir: " + ex.getMessage());
            }
        });

        btnStop.addActionListener(e -> {
            try {
                Stop();
                estadoReproductor = 0; // Cambia el estado a Detenido
                tiempoActualMilisegundos = 0;

                if (timerLetras != null) timerLetras.cancelar();
                lblLetraActual.setText("♪ ... (Presione Play para iniciar) ... ♪");
            } catch (Exception ex) {
                System.out.println("Error al detener: " + ex.getMessage());
            }
        });

        // --- LÓGICA DE PAUSA ACTUALIZADA ---
        btnPause.addActionListener(e -> {
            try {
                if (estadoReproductor == 1) { // Solo pausa si está reproduciendo
                    Pausa();
                    estadoReproductor = 2; // Cambia el estado a Pausado
                    if (timerLetras != null) timerLetras.cancelar();
                }
            } catch (Exception ex) {
                System.out.println("Error al pausar: " + ex.getMessage());
            }
        });

        panelBotones.add(btnCargar);
        panelBotones.add(btnStop);
        panelBotones.add(btnPlay);
        panelBotones.add(btnPause);

        panelInferior.add(panelBotones, BorderLayout.CENTER);
        frame.add(panelInferior, BorderLayout.SOUTH);
    }

    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(Color.DARK_GRAY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        return btn;
    }

    private void cargarArchivos() {
        File directorioActual = new File(System.getProperty("user.dir"));
        File[] archivosMp3 = directorioActual.listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".mp3"));

        if (archivosMp3 == null || archivosMp3.length == 0) {
            JOptionPane.showMessageDialog(frame, "No se encontraron canciones (.mp3) en la carpeta del proyecto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] nombresCanciones = new String[archivosMp3.length];
        for (int i = 0; i < archivosMp3.length; i++) {
            nombresCanciones[i] = archivosMp3[i].getName();
        }

        String seleccion = (String) JOptionPane.showInputDialog(
                frame, "Seleccione la canción a reproducir:", "Cargar MP3",
                JOptionPane.PLAIN_MESSAGE, null, nombresCanciones, nombresCanciones[0]);

        if (seleccion != null) {
            for (File archivoMp3 : archivosMp3) {
                if (archivoMp3.getName().equals(seleccion)) {
                    rutaArchivoAudio = archivoMp3.getAbsolutePath();
                    try {
                        AbrirFichero(rutaArchivoAudio);

                        // Reiniciamos variables de estado al cargar un nuevo archivo
                        estadoReproductor = 0;
                        tiempoActualMilisegundos = 0;
                        if (timerLetras != null) timerLetras.cancelar();
                        lblLetraActual.setText("♪ ... (Presione Play para iniciar) ... ♪");

                    } catch (Exception ex) {
                        System.out.println("Error abriendo fichero: " + ex.getMessage());
                    }

                    String rutaLrc = rutaArchivoAudio.substring(0, rutaArchivoAudio.lastIndexOf('.')) + ".lrc";
                    File archivoLrc = new File(rutaLrc);

                    if (archivoLrc.exists()) {
                        procesarArchivoLrc(archivoLrc);
                    } else {
                        JOptionPane.showMessageDialog(frame, "No se encontró un archivo .lrc asociado.");
                        repositorioLetras.clear();
                        lblTitulo.setText("Canción: " + archivoMp3.getName());
                    }
                    break;
                }
            }
        }
    }

    private void procesarArchivoLrc(File archivoLrc) {
        try {
            Lexer lexer = new Lexer(new PushbackReader(new FileReader(archivoLrc), 1024));
            Parser parser = new Parser(lexer);

            arbol = parser.parse();
            arbolMostrado = false;

            LrcVisitor visitor = new LrcVisitor();
            arbol.apply(visitor);

            this.repositorioLetras = visitor.getRepositorio();
            MetadataLetra meta = visitor.getMetadata();

            lblTitulo.setText("Canción: " + meta.getTitulo());
            lblArtista.setText("Artista: " + meta.getArtista());
            lblAlbum.setText("Álbum: " + meta.getAlbum());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error procesando el archivo .lrc:\n" + ex.getMessage());
        }
    }

    public void mostrarVentana() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // =================================================================
    // EVENTOS DEL BASIC PLAYER LISTENER
    // =================================================================

    @Override
    public void opened(Object stream, Map properties) {
        if (properties != null && properties.containsKey("duration")) {
            totalDuracionMicrosegundos = (Long) properties.get("duration");
        } else {
            totalDuracionMicrosegundos = -1;
        }
    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
        // --- AÑADIDO: Capturamos el milisegundo exacto para usarlo al reanudar ---
        tiempoActualMilisegundos = microseconds / 1000;

        if (totalDuracionMicrosegundos > 0) {
            int progreso = (int) ((microseconds * 100) / totalDuracionMicrosegundos);

            long currentSecs = microseconds / 1000000;
            long totalSecs = totalDuracionMicrosegundos / 1000000;

            String curTime = String.format("%02d:%02d", currentSecs / 60, currentSecs % 60);
            String totTime = String.format("%02d:%02d", totalSecs / 60, totalSecs % 60);

            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(progreso);
                progressBar.setString(curTime + " / " + totTime);
            });
        }
    }

    @Override
    public void stateUpdated(BasicPlayerEvent event) {
        // Asegura que al terminar la canción o forzar un stop, todo regrese a 0
        if (event.getCode() == BasicPlayerEvent.STOPPED || event.getCode() == BasicPlayerEvent.EOM) {
            estadoReproductor = 0;
            tiempoActualMilisegundos = 0;
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(0);
                progressBar.setString("00:00 / 00:00");
            });
        }
    }

    @Override
    public void setController(BasicController controller) {
    }

    // --- MAIN ---
    public static void main(String args[]){
        SwingUtilities.invokeLater(() -> {
            try {
                Reproductor mi_reproductor = new Reproductor();
                mi_reproductor.mostrarVentana();
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });
    }

    // --- CLASE REMINDER ACTUALIZADA PARA ACEPTAR EL DESFASE DE PAUSA ---
    public class Reminder {
        Timer timer;

        // Ahora recibe un offset (desfase) para saber cuánto tiempo ya pasó
        public Reminder(List<LineaLetra> letras, long offsetMilisegundos) {
            timer = new Timer();
            for (LineaLetra linea : letras) {
                // Se calcula cuánto falta para que aparezca la letra
                long tiempoRestante = linea.getMilisegundos() - offsetMilisegundos;

                // Si la letra debe aparecer en el futuro, se programa la alarma
                if (tiempoRestante >= 0) {
                    timer.schedule(new RemindTask(linea.getTexto()), tiempoRestante);
                }
            }
        }

        public void cancelar() {
            if (timer != null) {
                timer.cancel();
            }
        }

        class RemindTask extends TimerTask {
            String texto;

            public RemindTask(String texto) {
                this.texto = texto;
            }

            public void run() {
                SwingUtilities.invokeLater(() -> {
                    lblLetraActual.setText(texto);
                });
            }
        }
    }
}