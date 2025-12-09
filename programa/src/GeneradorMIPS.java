import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generador de código MIPS simplificado que recorre el Codigo3D línea por línea
 * y emite una salida destino.asm donde cada instrucción queda registrada y, en
 * los casos más comunes, traducida a plantillas MIPS.
 */
public class GeneradorMIPS {

    private enum Tipo {
        INT, FLOAT, BOOL, CHAR, STRING, UNKNOWN
    }

    private static final Pattern DECLARE_PATTERN =
            Pattern.compile("declare\\s+([^:]+):(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DECLARE_ARRAY_PATTERN =
            Pattern.compile("declare_arr\\s+([\\w]+)\\[(\\d+)\\]\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASTORE_PATTERN =
            Pattern.compile("astore\\s+([^,]+),\\s*([^,]+),\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARR_PAD_PATTERN =
            Pattern.compile("arr_pad_zero\\s+([^,]+),\\s*([^,]+),\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final String[] SUPPORTED_BIN_OPS = {"+", "-", "*", "/"};

    private final List<String> codigoIntermedio;
    private final Path destinoAsm;

    private final StringBuilder textSection = new StringBuilder();
    private final StringBuilder dataSection = new StringBuilder(".data\n");

    private final Map<String, Tipo> variableTypes = new HashMap<>();
    private final Map<String, Tipo> arrayElementTypes = new HashMap<>();
    private final Map<String, String> literalPool = new HashMap<>();
    private final List<String> paramBuffer = new ArrayList<>();

    private int literalCounter = 0;
    private int padLoopCounter = 0;

    /**
     * Objetivo: Inicializar un generador listo para transformar el código 3D a MIPS.
     * Entradas: codigoIntermedio con cada línea estructurada y destinoAsm con la ruta del archivo destino.
     * Salidas: Instancia con buffers y contadores preparados para generar.
     * Restricciones: Se asume que la lista no es nula y que destinoAsm es escribible.
     */
    private GeneradorMIPS(List<String> codigoIntermedio, Path destinoAsm) {
        this.codigoIntermedio = codigoIntermedio;
        this.destinoAsm = destinoAsm;
    }

    /**
     * Objetivo: Orquestar la lectura del Codigo3D y la emisión de destino.asm desde los artefactos existentes.
     * Entradas: Rutas opcionales a Codigo3D, destino.asm y tabla de símbolos.
     * Salidas: Archivo destino.asm generado dentro de output (si había código).
     * Restricciones: Requiere que Codigo3D.txt exista y sea legible y que el directorio destino permita escritura.
     */
    public static void generarDesdeArtefactos(Path codigo3DPath, Path destinoAsmPath, Path tablaSimbolosPath) {
        Path workspace = Paths.get("").toAbsolutePath();
        Path outputDir = workspace.resolve("output");

        Path codigo = codigo3DPath != null ? codigo3DPath : outputDir.resolve("Codigo3D.txt");
        Path destino = destinoAsmPath != null ? destinoAsmPath : outputDir.resolve("destino.asm");

        if (Files.notExists(codigo)) {
            System.err.println("No se encontró Codigo3D.txt en " + codigo.toAbsolutePath());
            return;
        }

        try {
            List<String> lineas = Files.readAllLines(codigo, StandardCharsets.UTF_8);
            if (lineas.isEmpty()) {
                System.out.println("Codigo3D.txt vacío; no se generó destino.asm.");
                return;
            }
            GeneradorMIPS generador = new GeneradorMIPS(lineas, destino);
            generador.generar();
        } catch (IOException e) {
            System.err.println("No se pudo leer Codigo3D.txt: " + e.getMessage());
        }
    }

    /**
     * Objetivo: Ejecutar la secuencia completa de generación (encabezado, traducción y escritura).
     * Entradas: Ninguna, usa el estado almacenado en la instancia.
     * Salidas: Se actualizan los buffers y finalmente se escribe el archivo destino.asm.
     * Restricciones: Debe llamarse una vez que codigoIntermedio y destinoAsm han sido configurados.
     */
    private void generar() {
        escribirEncabezado();
        traducirCodigo();
        escribirArchivo();
    }

    /**
     * Objetivo: Empezar el archivo destino con las secciones base y el label main requerido.
     * Entradas: Ninguna.
     * Salidas: Contenido inicial en textSection.
     * Restricciones: Debe ejecutarse antes de agregar instrucciones al segmento de texto.
     */
    private void escribirEncabezado() {
        textSection.append(".text\n");
        textSection.append(".globl main\n");
        textSection.append("main:\n");
    }

    /**
     * Objetivo: Recorrer cada línea del código intermedio y delegar su traducción a manejadores específicos.
     * Entradas: Lista codigoIntermedio ya cargada.
     * Salidas: Instrucciones MIPS añadidas a textSection.
     * Restricciones: Las líneas meta o comentarios se omiten; se asume formato estructurado.
     */
    private void traducirCodigo() {
        for (String rawLine : codigoIntermedio) {
            if (rawLine == null) {
                continue;
            }
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#!")) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            if (line.endsWith(":")) {
                textSection.append(line).append('\n');
                continue;
            }
            procesarInstruccion(line);
        }
    }

    /**
     * Objetivo: Clasificar una instrucción del 3D y enviarla al manejador correspondiente.
     * Entradas: instruction con la línea limpia (sin espacios exteriores).
     * Salidas: Mutaciones sobre dataSection o textSection según la instrucción.
     * Restricciones: Debe recibir cadenas no vacías; escribe advertencias para casos no soportados.
     */
    private void procesarInstruccion(String instruction) {
        Matcher declareMatcher = DECLARE_PATTERN.matcher(instruction);
        if (declareMatcher.matches()) {
            manejarDeclaracion(declareMatcher);
            return;
        }

        Matcher declareArrMatcher = DECLARE_ARRAY_PATTERN.matcher(instruction);
        if (declareArrMatcher.matches()) {
            manejarDeclaracionArreglo(declareArrMatcher);
            return;
        }

        if (instruction.startsWith("if ")) {
            manejarIf(instruction);
            return;
        }
        if (instruction.startsWith("goto ")) {
            manejarGoto(instruction);
            return;
        }
        if (instruction.startsWith("print ")) {
            manejarPrint(instruction);
            return;
        }
        if (instruction.startsWith("read ")) {
            manejarRead(instruction);
            return;
        }
        if (instruction.startsWith("param ")) {
            manejarParam(instruction);
            return;
        }
        if (instruction.startsWith("call ")) {
            manejarCall(instruction);
            return;
        }
        if (instruction.startsWith("return")) {
            manejarReturn(instruction);
            return;
        }
        if (instruction.startsWith("arr_pad_zero")) {
            manejarArrPad(instruction);
            return;
        }
        if (instruction.startsWith("astore")) {
            manejarAstore(instruction);
            return;
        }
        if (instruction.contains("=")) {
            manejarAsignacion(instruction);
            return;
        }

        textSection.append("# instrucción no traducida: ").append(instruction).append('\n');
    }

    /**
     * Objetivo: Registrar variables escalares declaradas en el 3D para reservar espacio en .data.
     * Entradas: matcher que coincide con la expresión de declare.
     * Salidas: Nuevas entradas en variableTypes y dataSection.
     * Restricciones: El matcher debe contener nombre y tipo válidos.
     */
    private void manejarDeclaracion(Matcher matcher) {
        String nombre = matcher.group(1).trim();
        Tipo tipo = mapearTipo(matcher.group(2));
        registrarVariable(nombre, tipo);
    }

    /**
     * Objetivo: Registrar arreglos declarados en el 3D asignando su espacio en memoria.
     * Entradas: matcher obtenido de DECLARE_ARRAY_PATTERN.
     * Salidas: Etiquetas en dataSection y tipado por elemento en arrayElementTypes.
     * Restricciones: El tamaño debe convertirse correctamente a entero.
     */
    private void manejarDeclaracionArreglo(Matcher matcher) {
        String nombre = matcher.group(1).trim();
        int longitud = Integer.parseInt(matcher.group(2).trim());
        Tipo tipo = mapearTipo(matcher.group(3));
        arrayElementTypes.put(nombre, tipo);
        int bytes = longitud * bytesPorTipo(tipo);
        dataSection.append(nombre).append(": .space ").append(bytes).append('\n');
    }

    /**
     * Objetivo: Resolver asignaciones generales del 3D (literals, temporales y llamadas).
     * Entradas: instruction con la forma "destino = expresión".
     * Salidas: Instrucciones MIPS que cargan y almacenan el resultado.
     * Restricciones: Se exige el símbolo '='; en caso contrario se agrega comentario de advertencia.
     */
    private void manejarAsignacion(String instruction) {
        int idx = instruction.indexOf('=');
        if (idx < 0) {
            textSection.append("# asignación sin '=': ").append(instruction).append('\n');
            return;
        }
        String destino = instruction.substring(0, idx).trim();
        String rhs = instruction.substring(idx + 1).trim();

        if (rhs.startsWith("call ")) {
            manejarAsignacionDesdeCall(destino, rhs.substring(5).trim());
            return;
        }

        for (String op : SUPPORTED_BIN_OPS) {
            String patron = " " + op + " ";
            int pos = rhs.indexOf(patron);
            if (pos > 0) {
                manejarAsignacionBinaria(destino, rhs, op, pos);
                return;
            }
        }

        manejarAsignacionLiteral(destino, rhs);
    }

    /**
     * Objetivo: Convertir asignaciones cuyo lado derecho es una llamada a función.
     * Entradas: destino a almacenar y llamada con el nombre y conteo de parámetros opcional.
     * Salidas: Secuencia de llamada y almacenamiento del valor de retorno.
     * Restricciones: El formato debe ser "id, numero" o solo el identificador de la función.
     */
    private void manejarAsignacionDesdeCall(String destino, String llamada) {
        String[] partes = llamada.split(",");
        String nombre = partes[0].trim();
        int args = partes.length > 1 ? safeIntParse(partes[1].trim()) : paramBuffer.size();
        ejecutarCall(destino, nombre, args);
    }

    /**
     * Objetivo: Traducir expresiones binarias (+, -, *, /) entre temporales o variables.
     * Entradas: destino a almacenar, cadena rhs (lado derecho completo de la expresión), operador detectado y posición del operador.
     * Salidas: Instrucciones que cargan operandos, ejecutan la operación y guardan el resultado.
     * Restricciones: Solo se admiten los operadores de SUPPORTED_BIN_OPS; otros se comentan.
     */
    private void manejarAsignacionBinaria(String destino, String rhs, String operador, int posOperador) {
        String izquierda = rhs.substring(0, posOperador).trim();
        String derecha = rhs.substring(posOperador + operador.length() + 2).trim();
        cargarOperando(izquierda, "$t0");
        cargarOperando(derecha, "$t1");

        switch (operador) {
            case "+":
                textSection.append("addu $t2, $t0, $t1\n");
                break;
            case "-":
                textSection.append("subu $t2, $t0, $t1\n");
                break;
            case "*":
                textSection.append("mul $t2, $t0, $t1\n");
                break;
            case "/":
                textSection.append("div $t0, $t1\n");
                textSection.append("mflo $t2\n");
                break;
            default:
                textSection.append("# operador no soportado: ").append(operador).append('\n');
                return;
        }

        registrarVariable(destino, Tipo.INT);
        textSection.append("sw $t2, ").append(destino).append('\n');
    }

    /**
     * Objetivo: Manejar asignaciones donde el lado derecho de la expresión es un literal o referencia directa.
     * Entradas: destino y el lado derecho ya separados.
     * Salidas: Cargas inmediatas, copias o referencias a literales en .data.
     * Restricciones: Usa inferencias simples, por lo que literales complejos no se soportan.
     */
    private void manejarAsignacionLiteral(String destino, String rhs) {
        Tipo tipoDestino = variableTypes.getOrDefault(destino, inferirTipo(rhs));
        registrarVariable(destino, tipoDestino);

        if (esBooleano(rhs) || esEntero(rhs)) {
            String valor = convertirLiteralEntero(rhs);
            textSection.append("li $t0, ").append(valor).append('\n');
            textSection.append("sw $t0, ").append(destino).append('\n');
            return;
        }

        if (variableTypes.containsKey(rhs)) {
            textSection.append("lw $t0, ").append(rhs).append('\n');
            textSection.append("sw $t0, ").append(destino).append('\n');
            return;
        }

        if (rhs.contains(" ") || tipoDestino == Tipo.STRING) {
            String literal = registrarLiteral(Tipo.STRING, rhs);
            textSection.append("la $t0, ").append(literal).append('\n');
            textSection.append("sw $t0, ").append(destino).append('\n');
            return;
        }

        textSection.append("# asignación literal no soportada: ").append(destino)
                   .append(" = ").append(rhs).append('\n');
    }

    /**
     * Objetivo: Traducir saltos condicionales tipo "if temp goto label".
     * Entradas: instruction completa.
     * Salidas: Instrucción MIPS bnez con la etiqueta destino.
     * Restricciones: Se requieren al menos cuatro tokens (if, cond, goto, label).
     */
    private void manejarIf(String instruction) {
        String[] partes = instruction.split("\\s+");
        if (partes.length < 4) {
            textSection.append("# if mal formado: ").append(instruction).append('\n');
            return;
        }
        String condicion = partes[1];
        String etiqueta = partes[3];
        cargarOperando(condicion, "$t0");
        textSection.append("bnez $t0, ").append(etiqueta).append('\n');
    }

    /**
     * Objetivo: Emitir saltos incondicionales a etiquetas generadas en el 3D.
     * Entradas: instruction con prefijo "goto".
     * Salidas: Instrucción MIPS "j etiqueta".
     * Restricciones: La etiqueta debe existir en el flujo generado.
     */
    private void manejarGoto(String instruction) {
        String etiqueta = instruction.substring(5).trim();
        textSection.append("j ").append(etiqueta).append('\n');
    }

    /**
     * Objetivo: Implementar la primitiva print diferenciando entre strings, chars e ints.
     * Entradas: instruction tras el prefijo "print".
     * Salidas: Llamadas a syscall con los códigos correspondientes.
     * Restricciones: Los strings inlines se guardan en .data; si no hay argumento se registra advertencia.
     */
    private void manejarPrint(String instruction) {
        String argumento = instruction.substring(5).trim();
        if (argumento.isEmpty()) {
            textSection.append("# print sin argumento\n");
            return;
        }
        Tipo tipo = tipoDeOperando(argumento);
        switch (tipo) {
            case STRING: {
                String literal = argumento;
                if (!variableTypes.containsKey(argumento)) {
                    literal = registrarLiteral(Tipo.STRING, argumento);
                    textSection.append("la $a0, ").append(literal).append('\n');
                } else {
                    textSection.append("lw $a0, ").append(argumento).append('\n');
                }
                textSection.append("li $v0, 4\n");
                textSection.append("syscall\n");
                break;
            }
            case CHAR:
                cargarOperando(argumento, "$a0");
                textSection.append("li $v0, 11\n");
                textSection.append("syscall\n");
                break;
            default:
                cargarOperando(argumento, "$a0");
                textSection.append("li $v0, 1\n");
                textSection.append("syscall\n");
                break;
        }
    }

    /**
     * Objetivo: Traducir instrucciones read asignándolas como lecturas enteras por syscall.
     * Entradas: instruction tras quitar el prefijo "read".
     * Salidas: Secuencia que invoca syscall 5 y almacena en la variable destino.
     * Restricciones: Solo soporta lecturas enteras; requiere nombre de variable válido.
     */
    private void manejarRead(String instruction) {
        String destino = instruction.substring(4).trim();
        if (destino.isEmpty()) {
            textSection.append("# read sin destino\n");
            return;
        }
        registrarVariable(destino, Tipo.INT);
        textSection.append("li $v0, 5\n");
        textSection.append("syscall\n");
        textSection.append("sw $v0, ").append(destino).append('\n');
    }

    /**
     * Objetivo: Almacenar argumentos para llamadas subsecuentes.
     * Entradas: instruction con el operando del param.
     * Salidas: Valores acumulados en paramBuffer.
     * Restricciones: Solo se añade cuando el operando no está vacío; el orden se conserva primero en entrar, primero en salir (first in, first out).
     */
    private void manejarParam(String instruction) {
        String argumento = instruction.substring(5).trim();
        if (!argumento.isEmpty()) {
            paramBuffer.add(argumento);
        }
    }

    /**
     * Objetivo: Emitir una llamada cuando no hay asignación directa al resultado.
     * Entradas: instruction con el nombre de la función y un conteo opcional.
     * Salidas: Código de llamada generado mediante ejecutarCall.
     * Restricciones: Si no se especifican argumentos se usa la cantidad almacenada en paramBuffer.
     */
    private void manejarCall(String instruction) {
        String resto = instruction.substring(4).trim();
        String[] partes = resto.split(",");
        String nombre = partes[0].trim();
        int args = partes.length > 1 ? safeIntParse(partes[1].trim()) : paramBuffer.size();
        ejecutarCall(null, nombre, args);
    }

    /**
     * Objetivo: Generar el protocolo de llamada (push de argumentos, jal y limpieza de stack).
     * Entradas: destino opcional, nombre de función y número de argumentos.
     * Salidas: Instrucciones que preparan la pila y guardan el valor de retorno si aplica.
     * Restricciones: Se asume convención simple; no se preservan registros del llamador.
     */
    private void ejecutarCall(String destino, String nombre, int argCount) {
        int inicio = Math.max(0, paramBuffer.size() - argCount);
        for (int i = inicio; i < paramBuffer.size(); i++) {
            String argumento = paramBuffer.get(i);
            cargarOperando(argumento, "$t0");
            textSection.append("addiu $sp, $sp, -4\n");
            textSection.append("sw $t0, 0($sp)\n");
        }
        textSection.append("jal ").append(nombre).append('\n');
        if (argCount > 0) {
            textSection.append("addiu $sp, $sp, ").append(argCount * 4).append('\n');
        }
        paramBuffer.clear();
        if (destino != null) {
            registrarVariable(destino, Tipo.INT);
            textSection.append("sw $v0, ").append(destino).append('\n');
        }
    }

    /**
     * Objetivo: Traducir returns ajustando $v0 cuando existe expresión de retorno.
     * Entradas: instruction completa del return.
     * Salidas: Cargas opcionales en $v0 y salto a jr $ra.
     * Restricciones: No administra limpieza de pila de activación.
     */
    private void manejarReturn(String instruction) {
        String resto = instruction.length() > 6 ? instruction.substring(6).trim() : "";
        if (!resto.isEmpty()) {
            cargarOperando(resto, "$v0");
        }
        textSection.append("jr $ra\n");
    }

    /**
     * Objetivo: Simular el relleno con ceros de arreglos parcialmente inicializados.
     * Entradas: instruction con nombre, índice inicial y tamaño total.
     * Salidas: Bucle MIPS que recorre el rango y escribe cero.
     * Restricciones: Requiere que el arreglo haya sido registrado para conocer el tamaño de elemento.
     */
    private void manejarArrPad(String instruction) {
        Matcher matcher = ARR_PAD_PATTERN.matcher(instruction);
        if (!matcher.matches()) {
            textSection.append("# arr_pad_zero no reconocido\n");
            return;
        }
        String nombre = matcher.group(1).trim();
        String inicio = matcher.group(2).trim();
        String total = matcher.group(3).trim();
        int bytes = bytesPorTipo(arrayElementTypes.getOrDefault(nombre, Tipo.INT));
        String etiquetaLoop = "arr_pad_" + padLoopCounter++;
        textSection.append("la $t0, ").append(nombre).append('\n');
        cargarOperando(inicio, "$t1");
        cargarOperando(total, "$t2");
        textSection.append(etiquetaLoop).append(":\n");
        textSection.append("beq $t1, $t2, ").append(etiquetaLoop).append("_end\n");
        textSection.append("sw $zero, 0($t0)\n");
        textSection.append("addi $t0, $t0, ").append(bytes).append('\n');
        textSection.append("addi $t1, $t1, 1\n");
        textSection.append("j ").append(etiquetaLoop).append('\n');
        textSection.append(etiquetaLoop).append("_end:\n");
    }

    /**
     * Objetivo: Traducir escrituras individuales sobre arreglos (astore en el 3D).
     * Entradas: instruction con arreglo, índice y valor.
     * Salidas: Código que calcula la dirección y almacena el dato.
     * Restricciones: Solo usa multiplicación por el tamaño del elemento; no valida límites.
     */
    private void manejarAstore(String instruction) {
        Matcher matcher = ASTORE_PATTERN.matcher(instruction);
        if (!matcher.matches()) {
            textSection.append("# astore no reconocido\n");
            return;
        }
        String arreglo = matcher.group(1).trim();
        String indice = matcher.group(2).trim();
        String valor = matcher.group(3).trim();
        int bytes = bytesPorTipo(arrayElementTypes.getOrDefault(arreglo, Tipo.INT));
        textSection.append("la $t0, ").append(arreglo).append('\n');
        cargarOperando(indice, "$t1");
        textSection.append("li $t3, ").append(bytes).append('\n');
        textSection.append("mul $t1, $t1, $t3\n");
        textSection.append("addu $t0, $t0, $t1\n");
        cargarOperando(valor, "$t2");
        textSection.append("sw $t2, 0($t0)\n");
    }

    /**
     * Objetivo: Cargar un operando arbitrario (literal, variable o string) en un registro destino.
     * Entradas: operando textual y nombre del registro donde depositarlo.
     * Salidas: Instrucciones de carga inmediata, lw o la según el caso.
     * Restricciones: Operandos desconocidos se convierten en cero con comentario aclaratorio.
     */
    private void cargarOperando(String operando, String registro) {
        operando = operando.trim();
        if (esBooleano(operando) || esEntero(operando)) {
            textSection.append("li ").append(registro).append(", ")
                       .append(convertirLiteralEntero(operando)).append('\n');
            return;
        }
        if (variableTypes.containsKey(operando)) {
            textSection.append("lw ").append(registro).append(", ")
                       .append(operando).append('\n');
            return;
        }
        if (operando.contains(" ")) {
            String literal = registrarLiteral(Tipo.STRING, operando);
            textSection.append("la ").append(registro).append(", ")
                       .append(literal).append('\n');
            return;
        }
        textSection.append("li ").append(registro)
                   .append(", 0 # operando no reconocido: ").append(operando)
                   .append('\n');
    }

    /**
     * Objetivo: Dar de alta variables escalares en el segmento .data solo una vez.
     * Entradas: nombre de la variable y tipo inferido.
     * Salidas: Declaración en dataSection y registro en variableTypes.
     * Restricciones: Si la variable ya existe se evita duplicar la entrada.
     */
    private void registrarVariable(String nombre, Tipo tipo) {
        if (variableTypes.containsKey(nombre)) {
            return;
        }
        variableTypes.put(nombre, tipo);
        dataSection.append(nombre).append(": ").append(directivaPara(tipo)).append('\n');
    }

    /**
     * Objetivo: Convertir etiquetas textuales del 3D a los valores del enum Tipo.
     * Entradas: raw con el nombre del tipo en mayúsculas o minúsculas.
     * Salidas: Valor correspondiente de Tipo.
     * Restricciones: Retorna UNKNOWN para tipos no contemplados.
     */
    private Tipo mapearTipo(String raw) {
        if (raw == null) {
            return Tipo.UNKNOWN;
        }
        switch (raw.trim().toUpperCase()) {
            case "INT":
                return Tipo.INT;
            case "FLOAT":
                return Tipo.FLOAT;
            case "BOOL":
            case "BOOLEAN":
                return Tipo.BOOL;
            case "CHAR":
                return Tipo.CHAR;
            case "STRING":
                return Tipo.STRING;
            default:
                return Tipo.UNKNOWN;
        }
    }

    /**
     * Objetivo: Determinar el tipo probable de un literal simple.
     * Entradas: literal tal como aparece en el 3D.
     * Salidas: Valor Tipo usado para decisiones posteriores.
     * Restricciones: Solo reconoce booleanos, enteros, floats con punto y chars de un caracter.
     */
    private Tipo inferirTipo(String literal) {
        if (esBooleano(literal)) {
            return Tipo.BOOL;
        }
        if (esEntero(literal)) {
            return Tipo.INT;
        }
        if (literal != null && literal.contains(".")) {
            return Tipo.FLOAT;
        }
        if (literal != null && literal.length() == 1) {
            return Tipo.CHAR;
        }
        return Tipo.STRING;
    }

    /**
     * Objetivo: Resolver el tipo de un operando dando prioridad a las variables registradas.
     * Entradas: Nombre del operando o literal.
     * Salidas: Tipo encontrado en variableTypes o inferido.
     * Restricciones: Depende de que registrarVariable se haya llamado previamente para las variables usadas.
     */
    private Tipo tipoDeOperando(String operando) {
        if (variableTypes.containsKey(operando)) {
            return variableTypes.get(operando);
        }
        return inferirTipo(operando);
    }

    /**
     * Objetivo: Seleccionar la directiva de .data apropiada según el tipo.
     * Entradas: tipo del enum.
     * Salidas: Cadena con la directiva (por ejemplo .word 0).
     * Restricciones: Tipos no contemplados caen en el caso por defecto (.word 0).
     */
    private String directivaPara(Tipo tipo) {
        switch (tipo) {
            case FLOAT:
                return ".float 0.0";
            case CHAR:
                return ".byte 0";
            case STRING:
                return ".word 0";
            default:
                return ".word 0";
        }
    }

    /**
     * Objetivo: Obtener el tamaño en bytes de cada tipo soportado.
     * Entradas: tipo del enum.
     * Salidas: Entero con el tamaño estimado.
     * Restricciones: Los tipos no definidos se tratan como de 4 bytes.
     */
    private int bytesPorTipo(Tipo tipo) {
        switch (tipo) {
            case CHAR:
                return 1;
            case FLOAT:
                return 4;
            case STRING:
                return 4;
            default:
                return 4;
        }
    }

    /**
     * Objetivo: Guardar literales en .data reutilizando etiquetas para valores repetidos.
     * Entradas: tipo del literal y su valor textual.
     * Salidas: Nombre de la etiqueta generada.
     * Restricciones: Usa contadores incrementales; no elimina literales nunca usados.
     */
    private String registrarLiteral(Tipo tipo, String valor) {
        String llave = tipo.name() + ":" + valor;
        if (literalPool.containsKey(llave)) {
            return literalPool.get(llave);
        }
        String label = tipo == Tipo.STRING ? "str_lit_" + literalCounter : "flt_lit_" + literalCounter;
        literalCounter++;
        if (tipo == Tipo.STRING) {
            dataSection.append(label).append(": .asciiz \"")
                       .append(escapar(valor)).append("\"\n");
        } else {
            dataSection.append(label).append(": .float ").append(valor).append('\n');
        }
        literalPool.put(llave, label);
        return label;
    }

    /**
     * Objetivo: Escapar barras y comillas para strings alojados en .data.
     * Entradas: valor original del literal.
     * Salidas: Cadena segura para .asciiz.
     * Restricciones: Solo reemplaza barra invertida y comillas dobles.
     */
    private String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Objetivo: Verificar si una cadena representa un entero con signo opcional.
     * Entradas: token textual.
     * Salidas: true si coincide con la expresión regular, false en otro caso.
     * Restricciones: Solo admite dígitos base 10.
     */
    private boolean esEntero(String token) {
        return token.matches("-?\\d+");
    }

    /**
     * Objetivo: Detectar literales booleanos en texto plano.
     * Entradas: token a evaluar.
     * Salidas: true para "true" o "false" (insensible a mayúsculas), false para lo demás.
     * Restricciones: No reconoce otras variantes como 0/1.
     */
    private boolean esBooleano(String token) {
        return "true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token);
    }

    /**
     * Objetivo: Convertir literales booleanos a enteros y dejar intactos los enteros recibidos.
     * Entradas: literal textual.
     * Salidas: "1", "0" o el mismo literal.
     * Restricciones: Solo mapea true/false; otros valores se retornan sin validar.
     */
    private String convertirLiteralEntero(String literal) {
        if ("true".equalsIgnoreCase(literal)) {
            return "1";
        }
        if ("false".equalsIgnoreCase(literal)) {
            return "0";
        }
        return literal;
    }

    /**
     * Objetivo: Parsear enteros de manera segura para valores auxiliares como conteos de parámetros.
     * Entradas: value en formato texto.
     * Salidas: Entero parseado o 0 si ocurre una excepción.
     * Restricciones: Silencia NumberFormatException, por lo que 0 puede significar error.
     */
    private int safeIntParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Objetivo: Persistir las secciones .data y .text en el archivo destino.
     * Entradas: Ninguna explícita; usa los StringBuilder acumulados.
     * Salidas: Archivo destino.asm escrito en UTF-8.
     * Restricciones: El directorio padre debe poder crearse; captura y reporta errores de E/S.
     */
    private void escribirArchivo() {
        StringBuilder salida = new StringBuilder();
        salida.append(dataSection);
        salida.append('\n').append(textSection);
        try {
            if (destinoAsm.getParent() != null) {
                Files.createDirectories(destinoAsm.getParent());
            }
            Files.writeString(destinoAsm, salida.toString(), StandardCharsets.UTF_8);
            System.out.println("Código MIPS guardado en: " + destinoAsm.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo escribir destino.asm: " + e.getMessage());
        }
    }
}
