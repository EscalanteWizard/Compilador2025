
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
            while (true) {
                token = lex.next_token();
                if (token.sym != 0) {
                    System.out.println("Token: " + token.sym + " " + (token.value == null ? lex.yytext() : token.value.toString()));
                    tokens.append("Token: " + token.sym + " " + (token.value == null ? lex.yytext() : token.value.toString()) + "\n");
                } else {
                    System.out.println("Cantidad de lexemas encontrados: " + i);
                    return tokens.toString(); // Convertir StringBuilder a String antes de devolverlo
                }
                i++;
            }
        }
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
        try (Reader reader = new BufferedReader(new FileReader(ruta))) {
            Lexer lex = new Lexer(reader);  // Crea un analizador léxico para el archivo
            Parser myParser = new Parser(lex);  // Crea un analizador sintáctico y le pasa el analizador léxico
            myParser.parse();  // Parsea el contenido del archivo
        }
    }
}
