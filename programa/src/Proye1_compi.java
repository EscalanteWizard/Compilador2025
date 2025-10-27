
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java_cup.runtime.Symbol;

public class Proye1_compi {
    /*
     * @param args the command line arguments       
     * @throws Exception
     * Si ocurre un error durante el análisis léxico o sintáctico, se lanza una excepción.
     */
    public static void main(String[] args) throws Exception {
        Path currentPath = Paths.get("");
        String currentDirectory = currentPath.toAbsolutePath().toString();

        // Asegurar que el directorio output existe
        Path outputDir = Paths.get(currentDirectory, "programa", "output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String inputPath = Paths.get(currentDirectory, "programa", "input", "test.txt").toString();

        // Análisis léxico: generar archivo TOKENS.txt en programa/output
        String tokens = test1(inputPath);
        WriteToFile(outputDir.resolve("TOKENS.txt").toString(), tokens);

        // Análisis sintáctico
        test2(inputPath);
    }

    /* Método para analizar léxico 
     * @param ruta Ruta del archivo a analizar
     * @return String con los tokens encontrados
    */
    public static String analizarLexico(String ruta) throws Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta))) {
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

    /* Método para analizar sintáctico
     * @param ruta Ruta del archivo a analizar
     * @throws Exception
     * Si ocurre un error durante el análisis léxico o sintáctico, se lanza una excepción.
    */
    public static void analizarSintactico(String ruta) throws Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta))) {
            Lexer lex = new Lexer(reader);
            Parser parser = new Parser(lex);
            parser.parse();
        }
    }

    /* Método para analizar léxico
     * @param ruta Ruta del archivo a analizar
     * @throws Exception
     * Si ocurre un error durante el análisis léxico, se lanza una excepción.
    */
    public static String test1(String ruta) throws FileNotFoundException, IOException, Exception {
        try (Reader reader = new BufferedReader(new FileReader(ruta))) {
            Lexer lex = new Lexer(reader);
            int i = 0;
            Symbol token;
            StringBuilder tokens = new StringBuilder();
            StringBuilder tabla = new StringBuilder();

            // Build reverse map tokenId -> name using reflection on sym
            java.util.Map<Integer,String> tokenNames = new java.util.HashMap<Integer,String>();
            try {
                for (java.lang.reflect.Field f : sym.class.getFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        Object val = f.get(null);
                        if (val instanceof Integer) tokenNames.put((Integer)val, f.getName());
                    }
                }
            } catch (Exception e) {
                // ignore, fallback to numeric ids
            }

            // Header for TablaSimbolos
            tabla.append("Tabla de Simbolos - Lista de tokens (Scope: SCOPE GLOBAL)\n");
            tabla.append("NombreLexema,TipoToken,Linea,Columna\n");

            while (true) {
                token = lex.next_token();
                if (token.sym != 0) {
                    String lexeme = (token.value == null ? lex.getYYText() : token.value.toString());
                    String tname = tokenNames.containsKey(token.sym) ? tokenNames.get(token.sym) : Integer.toString(token.sym);
                    int line = -1, col = -1;
                    try { line = lex.getLine(); col = lex.getColumn(); } catch (Exception ex) { /* ignore */ }
                    // make 1-based for human readers
                    String lineStr = (line >= 0 ? Integer.toString(line+1) : "n/a");
                    String colStr = (col >= 0 ? Integer.toString(col+1) : "n/a");

                    System.out.println("Token: " + token.sym + " " + lexeme + " (" + tname + ") at " + lineStr + ":" + colStr);
                    tokens.append("Token: " + token.sym + " " + lexeme + "\n");

                    // append to TablaSimbolos.csv-like
                    tabla.append(escapeCsv(lexeme) + "," + tname + "," + lineStr + "," + colStr + "\n");
                } else {
                    System.out.println("Cantidad de lexemas encontrados: " + i);
                    // write TablaSimbolos to output folder
                    Path out = Paths.get(Paths.get("").toAbsolutePath().toString(), "programa", "output");
                    Files.createDirectories(out);
                    Path tablaFile = out.resolve("TablaSimbolos.txt");
                    try (FileWriter fw = new FileWriter(tablaFile.toFile())) {
                        fw.write(tabla.toString());
                    }
                    System.out.println("Tabla de simbolos escrita en: " + tablaFile.toAbsolutePath().toString());
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

    /* Método para escribir en un archivo 
     * @param filename Nombre del archivo
     * @param content Contenido a escribir
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

    /* Método para analizar sintáctico
     * @param ruta Ruta del archivo a analizar
     * @throws Exception
     * Si ocurre un error durante el análisis léxico o sintáctico, se lanza una excepción.
    */
    public static void test2(String ruta) throws IOException, Exception {
        Reader reader = null;
        Parser myParser = null;
        try {
            reader = new BufferedReader(new FileReader(ruta));
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
                System.err.println("Error exportando tablas de simbolos: " + ex.getMessage());
            }
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { /* ignore */ }
            }
        }
    }
}
