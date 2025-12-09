
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java_cup.runtime.Symbol;
import java.nio.file.StandardCopyOption;

public class Proye1_compi {
    /**
     * Proposito: punto de entrada que coordina el analisis lexico y sintactico.
     * Entradas: argumento CLI opcional con la ruta del archivo fuente.
     * Salidas: genera TOKENS.txt, TablaSimbolos.txt y Codigo3D.txt dentro de output/.
     * Restricciones: requiere que el lexer y parser ya hayan sido generados; finaliza ante errores de E/S.
     */
    public static void main(String[] args) throws Exception {
        Path baseDir = resolveBaseDirectory();

        // Asegurar que el directorio output existe
        Path outputDir = baseDir.resolve("output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Determinar input path: usar argumento o default a test.txt
        String inputPath;
        if (args != null && args.length > 0) {
            Path provided = Paths.get(args[0]);
            inputPath = provided.isAbsolute() ? provided.toString() : baseDir.resolve(provided).toString();
        } else {
            inputPath = baseDir.resolve("input").resolve("test.txt").toString();
        }

        // Análisis léxico: generar archivo TOKENS.txt en programa/output
        String tokens = test1(inputPath);
        WriteToFile(outputDir.resolve("TOKENS.txt").toString(), tokens);

        // Análisis sintáctico
        test2(inputPath);

        // Generar archivo destino MIPS a partir del Codigo3D
        try {
            Path codigo3DPath = outputDir.resolve("Codigo3D.txt");
            Path destinoAsm = outputDir.resolve("destino.asm");
            Path tablaSimbolosPath = outputDir.resolve("TablaSimbolos.txt");
            GeneradorMIPS.generarDesdeArtefactos(codigo3DPath, destinoAsm, tablaSimbolosPath);
        } catch (Exception e) {
            System.err.println("No fue posible generar destino.asm: " + e.getMessage());
        }
    }

    /**
     * Proposito: detectar si la ejecucion inicia en la raiz del repositorio o dentro de /programa.
     * Entradas: ninguna; analiza el directorio de trabajo actual.
     * Salidas: ruta que apunta a la carpeta que contiene src/, input/ y output/.
     * Restricciones: regresa el directorio actual cuando la estructura no coincide.
     */
    private static Path resolveBaseDirectory() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src")) && Files.isDirectory(cwd.resolve("input"))) {
            return cwd;
        }
        Path nested = cwd.resolve("programa");
        if (Files.isDirectory(nested) && Files.isDirectory(nested.resolve("src")) && Files.isDirectory(nested.resolve("input"))) {
            return nested;
        }
        return cwd;
    }

    /**
     * Proposito: ejecutar el lexer una vez y capturar cada token para inspeccionarlo.
     * Entradas: ruta absoluta o relativa del archivo fuente.
     * Salidas: lista de tokens separada por saltos de linea y trazas en la consola.
     * Restricciones: quien llama debe manejar las excepciones de E/S o del lexer.
     */
    public static String analizarLexico(String ruta) throws Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta, StandardCharsets.UTF_8))) {
            Lexer lex = new Lexer(reader);
            StringBuilder tokens = new StringBuilder();
            Symbol token;
            int i = 0;
            while (true) {
                token = lex.next_token();
                if (token.sym != 0) {
                    tokens.append("Token: " + token.sym + " " + (token.value == null ? lex.yytext() : token.value.toString()) + "\n");
                } else {
                    break;
                }
                i++;
            }
            System.out.println("Cantidad de lexemas encontrados: " + i);
            return tokens.toString();
        }
    }

    /**
     * Proposito: ejecutar el parser sobre el archivo fuente indicado.
     * Entradas: ruta del programa fuente.
     * Salidas: ninguna directa; los errores se informan en stderr y las exportaciones las maneja el parser.
     * Restricciones: asume que el lexer y el parser ya estan disponibles en el classpath.
     */
    public static void analizarSintactico(String ruta) throws Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta, StandardCharsets.UTF_8))) {
            Lexer lex = new Lexer(reader);
            Parser parser = new Parser(lex);
            parser.parse();
        }
    }

    /**
     * Proposito: utilidad heredada que emite el listado de tokens para pruebas automatizadas.
     * Entradas: ruta del archivo que se desea analizar.
     * Salidas: cadena con los tokens formateados y trazas informativas en stdout.
     * Restricciones: lanza excepciones cuando el lexer falla; pensada para depuracion manual.
     */
    public static String test1(String ruta) throws FileNotFoundException, IOException, Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta, StandardCharsets.UTF_8))) {
            Lexer lex = new Lexer(reader);
            int i = 0;
            Symbol token;
            StringBuilder tokens = new StringBuilder();

            // Construir el mapa inverso tokenId -> nombre utilizando reflexion sobre sym
            java.util.Map<Integer,String> tokenNames = new java.util.HashMap<Integer,String>();
            try {
                for (java.lang.reflect.Field f : sym.class.getFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        Object val = f.get(null);
                        if (val instanceof Integer) tokenNames.put((Integer)val, f.getName());
                    }
                }
            } catch (Exception e) {
                // ignorar la excepcion y usar los identificadores numericos
            }

            while (true) {
                token = lex.next_token();
                if (token.sym != 0) {
                    String lexeme = (token.value == null ? lex.getYYText() : token.value.toString());
                    String tname = tokenNames.containsKey(token.sym) ? tokenNames.get(token.sym) : Integer.toString(token.sym);
                    int line = -1, col = -1;
                    try { line = lex.getLine(); col = lex.getColumn(); } catch (Exception ex) { /* ignorar */ }
                    // ajustar a base 1 para los lectores humanos
                    String lineStr = (line >= 0 ? Integer.toString(line+1) : "n/a");
                    String colStr = (col >= 0 ? Integer.toString(col+1) : "n/a");

                    System.out.println("Token: " + token.sym + " " + lexeme + " (" + tname + ") at " + lineStr + ":" + colStr);
                    tokens.append("Token: " + token.sym + " " + lexeme + " (" + tname + ") at " + lineStr + ":" + colStr + "\n");
                } else {
                    System.out.println("Cantidad de lexemas encontrados: " + i);
                    return tokens.toString(); // Convertir StringBuilder a String antes de devolverlo
                }
                i++;
            }
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * Proposito: persistir la salida de los analizadores en disco, creando directorios padres si hace falta.
     * Entradas: nombre del archivo destino y contenido textual.
     * Salidas: archivo escrito y mensaje en consola con la ruta absoluta.
     * Restricciones: sobrescribe archivos existentes; se espera contenido seguro en UTF-8.
     */
    public static void WriteToFile(String filename, String content) throws IOException {
        File file = new File(filename);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            System.out.println("El archivo se ha escrito correctamente en: " + file.getAbsolutePath());
        }   
    }

    /**
     * Proposito: utilidad heredada para ejecutar el parser y forzar la exportacion de tablas.
     * Entradas: ruta del archivo fuente.
     * Salidas: ninguna directa; los resultados se escriben mediante los ganchos del parser.
     * Restricciones: siempre intenta exportar aun si el parseo falla; quien llama maneja las excepciones.
     */
    public static void test2(String ruta) throws IOException, Exception {
        Reader reader = null;
        Parser myParser = null;
        try {
            reader = new BufferedReader(new FileReader(ruta, StandardCharsets.UTF_8));
            Lexer lex = new Lexer(reader);  // Crea un analizador léxico para el archivo
            myParser = new Parser(lex);  // Crea un analizador sintáctico y le pasa el analizador léxico
            myParser.parse();  // Parsea el contenido del archivo
        } catch (Exception e) {
            System.err.println("Exception during parse: " + e.getMessage());
        } finally {
            // Intentar exportar las tablas de simbolos aunque haya ocurrido un error de parseo
            try {
                if (myParser != null) {
                    myParser.imprimirscopePrograma();
                }
            } catch (Exception ex) {
                System.err.println("Error exportando resultados: " + ex.getMessage());
            }
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { /* ignorar */ }
            }
        }
    }
}
