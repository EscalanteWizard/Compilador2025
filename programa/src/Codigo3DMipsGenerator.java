import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proposito: transformar el Codigo3D generado por el parser en un listado MIPS basico.
 * Entradas: rutas del Codigo3D.txt y del archivo destinoMIPS.asm.
 * Salidas: archivo destinoMIPS.asm dentro de output/ con directivas .data y .text.
 * Restricciones: convierte solo un subconjunto de instrucciones; el resto se documenta como comentario.
 */
public class Codigo3DMipsGenerator {
    private static final Pattern SCALAR_DECL = Pattern.compile("declare(?:_global)?\\s+(\\w+):(\\w+)");
    private static final Pattern ARRAY_DECL = Pattern.compile("declare(?:_global)?_arr\\s+(\\w+)\\[(\\d+)\\]\\s+(\\w+)");
    private static final Pattern BINARY_EXPR = Pattern.compile("(.+?)\\s(\\+|-|\\*|/|&&|\\|\\||==|!=|<=|>=|<|>)\\s(.+)");
    private static final Pattern UNARY_EXPR = Pattern.compile("([!-])\\s*(.+)");

    private static final List<String> REGISTER_POOL = Arrays.asList(
        "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
        "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"
    );

    private final Map<String, String> registerMap = new LinkedHashMap<>();
    private final Set<String> memoryBackedSymbols = new LinkedHashSet<>();
    private final Map<String, String> symbolTypes = new LinkedHashMap<>();
    private int nextRegister = 0;

    public void generar(Path codigo3DPath, Path destinoMips) {
        try {
            if (!Files.exists(codigo3DPath)) {
                System.out.println("No se encontro Codigo3D.txt; se omite la traduccion a MIPS.");
                return;
            }

            List<String> rawLines = Files.readAllLines(codigo3DPath, StandardCharsets.UTF_8);
            if (rawLines.isEmpty()) {
                System.out.println("Codigo3D.txt esta vacio; se omite la traduccion a MIPS.");
                return;
            }

            StringBuilder data = new StringBuilder(".data\n");
            StringBuilder text = new StringBuilder(".text\n.globl main\nmain:\n");

            for (String raw : rawLines) {
                String line = raw.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (processDeclaration(line, data)) {
                    continue;
                }

                translateInstruction(line, text);
            }

            text.append("\n\t# Finaliza el programa\n\tli $v0, 10\n\tsyscall\n");

            StringBuilder asm = new StringBuilder();
            asm.append(data.toString().trim()).append(System.lineSeparator()).append(System.lineSeparator());
            asm.append(text.toString().trim()).append(System.lineSeparator());

            Files.createDirectories(destinoMips.getParent());
            Files.write(destinoMips, asm.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Codigo MIPS exportado a: " + destinoMips.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error generando destino MIPS: " + e.getMessage());
        }
    }

    private boolean processDeclaration(String line, StringBuilder data) {
        Matcher scalar = SCALAR_DECL.matcher(line);
        if (scalar.matches()) {
            String name = scalar.group(1);
            String type = scalar.group(2).toUpperCase();
            if (memoryBackedSymbols.add(name)) {
                symbolTypes.put(name, type);
                data.append(name).append(": ").append(directiveFor(type)).append(" ")
                    .append(defaultValue(type)).append("\n");
            }
            return true;
        }

        Matcher array = ARRAY_DECL.matcher(line);
        if (array.matches()) {
            String name = array.group(1);
            int length = Integer.parseInt(array.group(2));
            String type = array.group(3).toUpperCase();
            if (memoryBackedSymbols.add(name)) {
                symbolTypes.put(name, type + "[]");
                int bytes = Math.max(1, length) * bytesFor(type);
                data.append(name).append(": .space ").append(bytes)
                    .append(" # ").append(type).append("[").append(length).append("]\n");
            }
            return true;
        }
        return false;
    }

    private void translateInstruction(String line, StringBuilder text) {
        if ("begin_main".equals(line) || "end_main".equals(line)) {
            text.append("\t# ").append(line).append("\n");
            return;
        }

        if (line.startsWith("//")) {
            text.append("\t# ").append(line.substring(2).trim()).append("\n");
            return;
        }

        if (line.endsWith(":")) {
            text.append(line).append("\n");
            return;
        }

        if (line.startsWith("goto ")) {
            text.append("\tj ").append(line.substring(5).trim()).append("\n");
            return;
        }

        if (line.startsWith("if ")) {
            handleConditional(line, text);
            return;
        }

        if (line.startsWith("arr_pad_zero") || line.startsWith("param ") || line.startsWith("call ") || line.startsWith("return")) {
            text.append("\t# TODO: ").append(line).append("\n");
            return;
        }

        if (line.contains("=")) {
            handleAssignment(line, text);
            return;
        }

        text.append("\t# Instruccion no soportada: ").append(line).append("\n");
    }

    private void handleAssignment(String line, StringBuilder text) {
        String[] parts = line.split("=", 2);
        if (parts.length < 2) {
            text.append("\t# Asignacion invalida: ").append(line).append("\n");
            return;
        }

        String dest = parts[0].trim();
        String rhs = parts[1].trim();

        Matcher binary = BINARY_EXPR.matcher(rhs);
        if (binary.matches()) {
            translateBinary(dest, binary.group(1).trim(), binary.group(2).trim(), binary.group(3).trim(), text);
            return;
        }

        Matcher unary = UNARY_EXPR.matcher(rhs);
        if (unary.matches()) {
            translateUnary(dest, unary.group(1), unary.group(2).trim(), text);
            return;
        }

        translateMove(dest, rhs, text);
    }

    private void translateBinary(String dest, String left, String op, String right, StringBuilder text) {
        String rd = registerFor(dest);
        String rl = ensureOperand(left, text);
        String rr = ensureOperand(right, text);

        switch (op) {
            case "+":
                text.append("\tadd ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "-":
                text.append("\tsub ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "*":
                text.append("\tmul ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "/":
                text.append("\tdiv ").append(rl).append(", ").append(rr).append("\n");
                text.append("\tmflo ").append(rd).append("\n");
                break;
            case "&&":
                text.append("\tand ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "||":
                text.append("\tor ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "==":
                text.append("\tseq ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "!=":
                text.append("\tsne ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "<":
                text.append("\tslt ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case "<=":
                text.append("\tsle ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case ">":
                text.append("\tsgt ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            case ">=":
                text.append("\tsge ").append(rd).append(", ").append(rl).append(", ").append(rr).append("\n");
                break;
            default:
                text.append("\t# Operador no soportado: ").append(op).append(" en ").append(left).append(" ").append(op).append(" ").append(right).append("\n");
                return;
        }

        persistIfMemoryBacked(dest, rd, text);
    }

    private void translateUnary(String dest, String op, String operand, StringBuilder text) {
        String rd = registerFor(dest);
        String ro = ensureOperand(operand, text);

        if ("-".equals(op)) {
            text.append("\tneg ").append(rd).append(", ").append(ro).append("\n");
        } else if ("!".equals(op)) {
            text.append("\txori ").append(rd).append(", ").append(ro).append(", 1\n");
        } else {
            text.append("\t# Operador unario no soportado: ").append(op).append(" ").append(operand).append("\n");
            return;
        }

        persistIfMemoryBacked(dest, rd, text);
    }

    private void translateMove(String dest, String source, StringBuilder text) {
        String rd = registerFor(dest);
        if (isInteger(source)) {
            text.append("\tli ").append(rd).append(", ").append(source).append("\n");
        } else if (isBooleanLiteral(source)) {
            text.append("\tli ").append(rd).append(", ").append(Boolean.parseBoolean(source) ? 1 : 0).append("\n");
        } else if (source.startsWith("\"") && source.endsWith("\"")) {
            text.append("\t# TODO: mover cadenas: ").append(source).append("\n");
        } else {
            String rs = ensureOperand(source, text);
            if (!rd.equals(rs)) {
                text.append("\tmove ").append(rd).append(", ").append(rs).append("\n");
            }
        }
        persistIfMemoryBacked(dest, rd, text);
    }

    private void handleConditional(String line, StringBuilder text) {
        String body = line.substring(2).trim();
        int gotoIdx = body.indexOf("goto");
        if (gotoIdx == -1) {
            text.append("\t# If mal formado: ").append(line).append("\n");
            return;
        }
        String condition = body.substring(0, gotoIdx).trim();
        String label = body.substring(gotoIdx + 4).trim();
        String reg = ensureOperand(condition, text);
        text.append("\tbne ").append(reg).append(", $zero, ").append(label).append("\n");
    }

    private String ensureOperand(String operand, StringBuilder text) {
        operand = operand.trim();
        if (isInteger(operand)) {
            text.append("\tli $at, ").append(operand).append("\n");
            return "$at";
        }
        if (isBooleanLiteral(operand)) {
            text.append("\tli $at, ").append(Boolean.parseBoolean(operand) ? 1 : 0).append("\n");
            return "$at";
        }
        String reg = registerFor(operand);
        if (memoryBackedSymbols.contains(operand)) {
            text.append("\tlw ").append(reg).append(", ").append(operand).append("\n");
        }
        return reg;
    }

    private String registerFor(String symbol) {
        if (registerMap.containsKey(symbol)) {
            return registerMap.get(symbol);
        }
        String reg;
        if (nextRegister < REGISTER_POOL.size()) {
            reg = REGISTER_POOL.get(nextRegister++);
        } else {
            reg = REGISTER_POOL.get(REGISTER_POOL.size() - 1);
        }
        registerMap.put(symbol, reg);
        return reg;
    }

    private void persistIfMemoryBacked(String symbol, String register, StringBuilder text) {
        if (memoryBackedSymbols.contains(symbol)) {
            text.append("\tsw ").append(register).append(", ").append(symbol).append("\n");
        }
    }

    private String directiveFor(String type) {
        switch (type) {
            case "FLOAT":
                return ".float";
            case "CHAR":
                return ".byte";
            case "STRING":
                return ".asciiz";
            default:
                return ".word";
        }
    }

    private String defaultValue(String type) {
        switch (type) {
            case "FLOAT":
                return "0.0";
            case "STRING":
                return "\"\"";
            default:
                return "0";
        }
    }

    private int bytesFor(String type) {
        switch (type) {
            case "CHAR":
                return 1;
            case "FLOAT":
                return 4;
            default:
                return 4;
        }
    }

    private boolean isInteger(String value) {
        return value.matches("-?\\d+");
    }

    private boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
}
