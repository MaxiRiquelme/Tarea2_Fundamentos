package tarea2;

import tarea2.lexer.Lexer;
import tarea2.parser.Parser;
import tarea2.node.Start;
import java.io.FileReader;
import java.io.PushbackReader;

public class Analizer {
    public static void main(String[] arguments) {
        try {
            // 1. Cargamos el archivo .lrc de ejemplo
            FileReader fileReader = new FileReader("The Weeknd - Blinding Lights.lrc");
            PushbackReader pushbackReader = new PushbackReader(fileReader, 1024);

            // 2. Instanciamos el Lexer y el Parser generados en el paquete tarea2
            Lexer lexer = new Lexer(pushbackReader);
            Parser parser = new Parser(lexer);

//            // 3. Se ejecuta el análisis sintáctico (Bottom-Up) y se construye el árbol
//            Start tree = parser.parse();
//
//            System.out.println("¡Análisis sintáctico exitoso! Imprimiendo árbol...\n");
//
//            // 4. Se aplica el visitador para imprimir el árbol en consola
//            tree.apply(new ASTPrinter());

            // 3. Se ejecuta el análisis sintáctico (Bottom-Up) y se construye el árbol
            Start tree = parser.parse();

            // 4. CREAMOS Y APLICAMOS NUESTRO VISITADOR
            LrcVisitor miVisitador = new LrcVisitor();
            tree.apply(miVisitador);

            // 5. Comprobamos que haya guardado los datos
            System.out.println("Extracción exitosa. Mostrando tiempos en milisegundos:");
            for(LineaLetra linea : miVisitador.listaLetras) {
                System.out.println("A los " + linea.tiempoMs + " ms -> " + linea.texto);
            }

        } catch (Exception e) {
            System.out.println("Error durante el parsing: ");
            e.printStackTrace();
        }
    }
}