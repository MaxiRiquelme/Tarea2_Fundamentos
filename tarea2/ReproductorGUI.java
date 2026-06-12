package tarea2;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import tarea2.lexer.Lexer;
import tarea2.parser.Parser;
import tarea2.node.Start;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.PushbackReader;
import java.util.ArrayList;

public class ReproductorGUI extends JFrame {

    // --- COMPONENTES DE LA INTERFAZ GRÁFICA ---
    private JLabel lblTitulo;
    private JLabel lblArtista;
    private JLabel lblLetras;
    private JProgressBar pbTiempo;
    private JButton btnCargar, btnStop, btnPlay, btnPause;

    // --- LÓGICA DE AUDIO Y COMPILADORES ---
    private BasicPlayer player;
    private ArrayList<LineaLetra> letrasActuales;

    // --- VARIABLES DE CONTROL DE TIEMPO (SINCRONIZACIÓN) ---
    private Timer uiTimer;               // Reloj de la interfaz gráfica (Swing Timer)
    private long totalPlayedTime = 0;    // Tiempo total reproducido acumulado (ms)
    private long lastResumeTime = 0;     // Marca de tiempo del último "Play" (ms)
    private boolean isPlaying = false;    // Estado actual de la reproducción
    private long duracionTotalMs = 1;    // Duración estimada de la canción para la barra

    // --- COLORES DE DISEÑO (Estilo Oscuro Personalizado) ---
    private Color colorFondo = new Color(24, 24, 24);        // Gris oscuro para el fondo
    private Color colorBoton = new Color(40, 40, 40);        // Gris medio para los botones
    private Color colorLetraKaraoke = new Color(30, 215, 96); // Verde brillante para la letra activa
    private Color colorTextoBlanco = Color.WHITE;            // Blanco para títulos principales

    public ReproductorGUI() {
        // 1. Configuración básica de la ventana principal
        setTitle("Reproductor");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar la ventana en la pantalla

        // 2. Inicializar el motor de audio y el repositorio de letras
        player = new BasicPlayer();
        letrasActuales = new ArrayList<>();

        // 3. Crear el panel principal con un diseño de bordes (BorderLayout)
        JPanel panelMain = new JPanel(new BorderLayout());
        panelMain.setBackground(colorFondo);
        setContentPane(panelMain);

        // 4. CONSTRUIR PANEL SUPERIOR: Información del archivo (Título y Artista)
        JPanel panelTop = new JPanel(new GridLayout(2, 1));
        panelTop.setBackground(colorFondo);
        panelTop.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        lblTitulo = new JLabel("Canción: (Ninguna)", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitulo.setForeground(colorTextoBlanco);

        lblArtista = new JLabel("Artista: (Ninguno)", SwingConstants.CENTER);
        lblArtista.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblArtista.setForeground(Color.LIGHT_GRAY);

        panelTop.add(lblTitulo);
        panelTop.add(lblArtista);

        // 5. CONSTRUIR PANEL CENTRAL: Visualizador gigante de las letras
        JPanel panelCenter = new JPanel(new BorderLayout());
        panelCenter.setBackground(colorFondo);

        lblLetras = new JLabel("Selecciona una canción para comenzar", SwingConstants.CENTER);
        lblLetras.setFont(new Font("SansSerif", Font.BOLD, 34));
        lblLetras.setForeground(colorLetraKaraoke);
        panelCenter.add(lblLetras, BorderLayout.CENTER);

        // 6. CONSTRUIR PANEL INFERIOR: Barra de progreso y botonera de control
        JPanel panelBottom = new JPanel(new BorderLayout());
        panelBottom.setBackground(colorFondo);
        panelBottom.setBorder(BorderFactory.createEmptyBorder(10, 30, 30, 30));

        // Configuración de la barra de progreso (Timeline)
        pbTiempo = new JProgressBar(0, 100);
        pbTiempo.setStringPainted(true);
        pbTiempo.setForeground(colorLetraKaraoke);
        pbTiempo.setBackground(Color.DARK_GRAY);

        // Configuración de los botones contenedores
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panelBotones.setBackground(colorFondo);

        btnCargar = crearBotonEstilizado("📁 Cargar MP3");
        btnStop   = crearBotonEstilizado("⏹ Stop");
        btnPlay   = crearBotonEstilizado("▶ Play");
        btnPause  = crearBotonEstilizado("⏸ Pause");

        panelBotones.add(btnCargar);
        panelBotones.add(btnStop);
        panelBotones.add(btnPlay);
        panelBotones.add(btnPause);

        panelBottom.add(pbTiempo, BorderLayout.NORTH);
        panelBottom.add(panelBotones, BorderLayout.CENTER);

        // 7. Ensamblar todas las secciones en el panel raíz
        panelMain.add(panelTop, BorderLayout.NORTH);
        panelMain.add(panelCenter, BorderLayout.CENTER);
        panelMain.add(panelBottom, BorderLayout.SOUTH);

        // 8. Activar los controladores de acciones y el reloj de sincronización
        configurarEventos();
        inicializarRelojSincronizador();
    }

    // Método auxiliar para construir botones uniformes y con apariencia limpia
    private JButton crearBotonEstilizado(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(colorBoton);
        btn.setForeground(colorTextoBlanco);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 15)
        ));
        return btn;
    }

    // Vinculación de los clics de botones a sus respectivas funciones
    private void configurarEventos() {
        btnCargar.addActionListener(e -> seleccionarCancion());
        btnPlay.addActionListener(e -> reproducir());
        btnPause.addActionListener(e -> pausar());
        btnStop.addActionListener(e -> detener());
    }

    // --- RELOJ INTERNO DE LA INTERFAZ GRÁFICA ---
    private void inicializarRelojSincronizador() {
        // Creamos un temporizador que "late" cada 100 milisegundos para refrescar la pantalla
        uiTimer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    // 1. Calcular el tiempo actual exacto sumando lo acumulado y el delta actual
                    long tiempoActualMs = totalPlayedTime + (System.currentTimeMillis() - lastResumeTime);

                    // 2. Actualizar el porcentaje de la barra de progreso
                    int progreso = (int) ((tiempoActualMs * 100) / duracionTotalMs);
                    pbTiempo.setValue(Math.min(progreso, 100));

                    // 3. Buscar de manera secuencial qué letra corresponde al milisegundo actual
                    String letraVisible = "";
                    for (LineaLetra linea : letrasActuales) {
                        if (tiempoActualMs >= linea.tiempoMs) {
                            letraVisible = linea.texto;
                        } else {
                            // Al estar ordenadas cronológicamente, si pasamos el tiempo actual, rompemos el ciclo
                            break;
                        }
                    }

                    // 4. Pintar el resultado en el JLabels del centro
                    if (letraVisible.isEmpty()) {
                        lblLetras.setText("♪ ... (Instrumental) ... ♪");
                    } else {
                        lblLetras.setText(letraVisible);
                    }
                }
            }
        });
    }

    // --- INTEGRACIÓN CON SABLECC Y AUDIO ---
    private void seleccionarCancion() {
        // 1. Apuntamos a la carpeta raíz del proyecto
        File carpetaRaiz = new File(".");

        // 2. Escaneamos la carpeta buscando SOLO archivos que terminen en .mp3
        File[] archivosMp3 = carpetaRaiz.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        // 3. Validamos que existan canciones en la carpeta
        if (archivosMp3 == null || archivosMp3.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No se encontraron canciones (.mp3) en la carpeta principal del proyecto.\n" +
                            "Asegúrate de pegar los archivos ahí.",
                    "Sin música", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4. Creamos un arreglo con los nombres de las canciones para mostrarlos
        String[] nombresCanciones = new String[archivosMp3.length];
        for (int i = 0; i < archivosMp3.length; i++) {
            nombresCanciones[i] = archivosMp3[i].getName();
        }

        // 5. Mostramos una ventana emergente elegante con una lista desplegable
        String cancionElegida = (String) JOptionPane.showInputDialog(
                this,
                "Elige la canción que deseas escuchar:",
                "🎵 Biblioteca de Música",
                JOptionPane.QUESTION_MESSAGE,
                null, // Ícono por defecto
                nombresCanciones, // La lista de opciones
                nombresCanciones[0] // El que sale seleccionado por defecto
        );

        // 6. Si el usuario eligió una canción (no apretó Cancelar)
        if (cancionElegida != null) {
            File mp3File = new File(carpetaRaiz, cancionElegida);
            try {
                // Buscamos el archivo de letra (.lrc o .lrc.txt) con el mismo nombre
                String lrcPath = mp3File.getAbsolutePath().replace(".mp3", ".lrc.txt");
                File lrcFile = new File(lrcPath);

                if (!lrcFile.exists()) {
                    lrcPath = mp3File.getAbsolutePath().replace(".mp3", ".lrc");
                    lrcFile = new File(lrcPath);
                }

                // Si encontramos la letra, llamamos al compilador SableCC y al reproductor
                if (lrcFile.exists()) {
                    cargarLetraConSableCC(lrcFile);
                    player.open(mp3File);

                    lblLetras.setText("¡Listo para reproducir!");
                    detener(); // Reseteamos la barra de tiempo y los contadores
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No se encontró el archivo de letra (.lrc) asociado a la canción.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los archivos.");
                ex.printStackTrace();
            }
        }
    }

    // Método que encapsula el análisis sintáctico y semántico del compilador
    private void cargarLetraConSableCC(File lrcFile) throws Exception {
        FileReader fileReader = new FileReader(lrcFile);
        PushbackReader pushbackReader = new PushbackReader(fileReader, 1024);

        // Inicializamos los componentes del compilador Bottom-Up generados
        Lexer lexer = new Lexer(pushbackReader);
        Parser parser = new Parser(lexer);

        // Construimos el árbol sintáctico abstracto (AST)
        Start tree = parser.parse();

        // Aplicamos el visitador actualizado para recuperar letras y metadatos
        LrcVisitor miVisitador = new LrcVisitor();
        tree.apply(miVisitador);

        // Traspasamos la información extraída a nuestra clase gráfica
        this.letrasActuales = miVisitador.listaLetras;

        // Actualizamos los textos superiores con los metadatos semánticos leídos
        lblTitulo.setText("Canción: " + miVisitador.titulo);
        lblArtista.setText("Artista: " + miVisitador.artista);

        // Estimamos la duración total tomando el último registro de tiempo de la letra + 10 segundos
        if (!letrasActuales.isEmpty()) {
            duracionTotalMs = letrasActuales.get(letrasActuales.size() - 1).tiempoMs + 10000;
        } else {
            duracionTotalMs = 1;
        }
    }

    private void reproducir() {
        try {
            if (!isPlaying && player != null && !letrasActuales.isEmpty()) {
                // Si el tiempo acumulado es cero es un inicio limpio, de lo contrario reanudamos
                if (totalPlayedTime == 0) {
                    player.play();
                } else {
                    player.resume();
                }

                // Guardamos el momento exacto en que se inició/reanudó el audio
                lastResumeTime = System.currentTimeMillis();
                isPlaying = true;
                uiTimer.start(); // Encendemos el refresco de pantalla
            }
        } catch (BasicPlayerException ex) {
            ex.printStackTrace();
        }
    }

    private void pausar() {
        try {
            if (isPlaying && player != null) {
                player.pause();

                // Almacenamos el tramo transcurrido en la variable acumuladora antes de congelar
                totalPlayedTime += (System.currentTimeMillis() - lastResumeTime);

                isPlaying = false;
                uiTimer.stop(); // Detenemos el reloj para congelar la letra en pantalla
            }
        } catch (BasicPlayerException ex) {
            ex.printStackTrace();
        }
    }

    private void detener() {
        try {
            if (player != null) {
                player.stop();
                isPlaying = false;
                uiTimer.stop();

                // Reiniciamos todas las variables de control cronológico a cero
                totalPlayedTime = 0;
                pbTiempo.setValue(0);
                lblLetras.setText("...");
            }
        } catch (BasicPlayerException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Forzamos a Swing a usar el diseño multiplataforma estándar (Metal)
        // Esto garantiza que los sistemas operativos no alteren nuestra paleta de colores oscura
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lanzamos la aplicación de forma segura en el hilo de despacho de eventos de Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ReproductorGUI app = new ReproductorGUI();
                app.setVisible(true);
            }
        });
    }
}