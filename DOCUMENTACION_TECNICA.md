# Documentación Técnica - Validaciones Semánticas y Código 3D

## Tabla de Contenidos

1. [Arquitectura General](#arquitectura-general)
2. [Validaciones Semánticas](#validaciones-semánticas)
3. [Generación de Código 3D](#generación-de-código-3d)
4. [Estructura de Datos](#estructura-de-datos)
5. [Ejemplos de Ejecución](#ejemplos-de-ejecución)
6. [Referencias](#referencias)

---

## Arquitectura General

### Flujo de Compilación

```
Entrada (archivo.txt)
    ↓
┌─────────────────────┐
│  Análisis Léxico    │ (JFlex → lexer.flex)
│  (tokenización)     │
└──────────┬──────────┘
           ↓
        TOKENS
           ↓
┌─────────────────────────────────────┐
│  Análisis Sintáctico                │ (CUP → sintax.cup)
│  + Validaciones Semánticas          │
│  + Generación de Código 3D          │
│  + Construcción Tabla de Símbolos   │
└──────────┬──────────────────────────┘
           ↓
    ┌──────────────┬──────────────┬─────────────┐
    ↓              ↓              ↓             ↓
TOKENS.txt  TablaSimbolos.txt  Codigo3D.txt  (output/)
```

---

## Validaciones Semánticas

### 1. Operaciones Aritméticas - compAritOp

**Producciones**:
```cup
compAritOp ::= term:op2 {: RESULT = op2; :}
             | compAritOp:op1 MAS term:op2 { validar y generar código }
             | compAritOp:op1 MENOS term:op2 { validar y generar código }
```

**Validaciones**:
```java
String[] elementos1 = op1.toString().split(":", -1);  // [valor, tipo]
String[] elementos2 = op2.toString().split(":", -1);

// 1. Verificar NULL (variable no declarada)
if (elementos1[1].equals("NULL") || elementos2[1].equals("NULL")) {
    error("Tipo NULL detectado");
    RESULT = "NULL:NULL";
}

// 2. Verificar tipos iguales
else if (!elementos1[1].equals(elementos2[1])) {
    error("Operandos de tipos diferentes");
    RESULT = "NULL:NULL";
}

// 3. Rechazar STRING
else if (elementos1[1].equals("STRING")) {
    error("No se puede sumar STRING");
    RESULT = "NULL:NULL";
}

// 4. Generar código 3D
else {
    String tempId = generarTemporal(tipo);
    cod3D.append("\n" + tempId + " = " + elementos1[0] + " + " + elementos2[0]);
    RESULT = tempId + ":" + tipo;
}
```

**Código 3D Generado**:
```
t1 = x + y
t2 = t1 * 2
t3 = a - t2
```

### 2. Operaciones Multiplicativas - term

Similar a compAritOp pero para `*, /, ^, %`

**Validaciones Adicionales**:
- Módulo (`%`) solo aplica para INT
- Rechaza BOOL y CHAR en todas operaciones

**Código 3D**:
```
t1 = a * b
t2 = t1 / 2
t3 = x ^ 3
t4 = n % 10
```

### 3. Asignación de Variables - varAsig

**Producción**:
```cup
varAsig ::= IDENTIFIER:id EQ exprP:expr DOLLAR {: validar y generar :}
```

**Validaciones**:
```java
String tipoVar = getTipo(id.toString());  // Buscar en tabla de símbolos
String[] elementosExpr = expr.toString().split(":", -1);

if (tipoVar.equals("null")) {
    error("Variable " + id + " no declarada");
}
else if (!tipoVar.equals(elementosExpr[1])) {
    error("Tipo " + tipoVar + " != " + elementosExpr[1]);
}
else {
    cod3D.append("\n" + id + " = " + elementosExpr[0]);
}
```

**Código 3D**:
```
x = 10
y = t1
z = 3.14
```

### 4. Comparaciones - exprLog

**Producción**:
```cup
exprLog ::= compAritOp:op1 opRel compAritOp:op2 {: validar :}
```

**Validaciones**:
- Operandos deben tener mismo tipo
- No permitir NULL

**Código 3D**:
```
if x > y goto true_label
```

### 5. Operaciones Lógicas - exprUni

**Validaciones**:
- AND/OR solo con operandos BOOL
- Variables usadas deben estar declaradas

### 6. Entrada/Salida

#### inputStruct
```cup
inputStruct ::= INPUT UKRA IDENTIFIER:id RUSS DOLLAR {: validar :}
```
- Variable debe estar declarada
- No permitir lectura directa a array

**Código 3D**: `read variable`

#### outPutStruct
```cup
outPutStruct ::= OUTPUT UKRA IDENTIFIER:id RUSS {: validar :}
```
- Variable debe estar declarada
- Soporta literales STRING y CHAR

**Código 3D**: 
```
write variable
write "literal"
```

### 7. Acceso a Arrays - arrayElement

**Producciones**:
```cup
arrayElement ::= IDENTIFIER:id SQUARES INT_LITERAL SQUAREC { validar }
               | IDENTIFIER:id SQUARES compAritOp SQUAREC { validar }
```

**Validaciones**:
- Array debe estar declarado
- Índice debe ser INT
- Ambas variantes (literal e índice dinámico)

---

## Generación de Código 3D

### Sistema de Temporales

#### Contadores Globales
```java
Integer currentTemp = 0;    // Para INT
Integer currentFloat = 0;   // Para FLOAT
```

#### Algoritmo de Generación

```java
String baseTemp = tipo.equals("FLOAT") ? "f" : "t";

if (tipo.equals("FLOAT")) {
    currentFloat++;
    tempId = "f" + currentFloat;
} else {
    currentTemp++;
    tempId = "t" + currentTemp;
}

cod3D.append("\n" + tempId + " = " + operando1 + " OP " + operando2);
RESULT = tempId + ":" + tipo;
```

#### Ejemplo de Secuencia
```
Input:  z = x + y * 2

Paso 1: y * 2      → t1 = y * 2
Paso 2: x + t1     → t2 = x + t1
Paso 3: z = t2     → z = t2

Output:
t1 = y * 2
t2 = x + t1
z = t2
```

### Exportación de Código 3D

**Método**: `exportarCodigo3D()` en Parser

```java
public void exportarCodigo3D() {
    Path outDir = Paths.get(..., "programa", "output");
    Files.createDirectories(outDir);
    
    Path cod3DFile = outDir.resolve("Codigo3D.txt");
    try (FileWriter fw = new FileWriter(cod3DFile.toFile())) {
        String codigo = cod3D.toString();
        if (codigo.startsWith("\n")) {
            codigo = codigo.substring(1);  // Limpia primer salto
        }
        fw.write(codigo);
    }
}
```

---

## Estructura de Datos

### Formato de Retorno de Producciones

Todas las producciones retornan un string con formato:
```
"valor:tipo"
```

**Ejemplos**:
- `"10:INT"` - Literal entero
- `"3.14:FLOAT"` - Literal flotante
- `"x:INT"` - Variable INT
- `"t1:INT"` - Temporal INT
- `"true:BOOL"` - Literal booleano
- `"variable_string:STRING"` - Literal string
- `"NULL:NULL"` - Error (tipo no determinado)

### Tabla de Símbolos

**Estructura**: `HashMap<String, ArrayList<String>>`

**Entrada**:
```
Scope: "MAIN"
Entry: "Instancia: x:INT:line=3:col=10"
```

**Búsqueda**:
```java
public String getTipo(String ID) {
    // Buscar en scope actual
    for (String var : scopePrograma.get(currentHash)) {
        String[] parts = var.split(":");
        if (parts[1].trim().equals(ID)) {
            return parts[2];  // Retorna tipo (INT, FLOAT, etc.)
        }
    }
    
    // Buscar en scope global
    for (String var : scopePrograma.get("SCOPE GLOBAL")) {
        String[] parts = var.split(":");
        if (parts[1].trim().equals(ID)) {
            return parts[2];
        }
    }
    
    return "null";  // No encontrado
}
```

---

## Ejemplos de Ejecución

### Ejemplo 1: Operaciones Aritméticas Válidas

**Entrada**:
```
int main ¿
    let int a$
    let int b$
    let int c$
    
    a = 10$
    b = 5$
    c = a + b$
    
    return 0$
?
```

**Salida (validaciones)**:
```
Asignacion correcta: a = null
Asignacion correcta: b = null
Asignacion correcta: c = t1
```

**Código 3D**:
```
a = null
b = null
t1 = a + b
c = t1
```

### Ejemplo 2: Tipo Mismatch

**Entrada**:
```
int main ¿
    let int x$
    let float y$
    x = y + 5$
    return 0$
?
```

**Salida (error)**:
```
Error semantico en SUMA (línea 4): Operandos de tipos diferentes (FLOAT y INT)
Error semantico en asignacion (línea 4): Expresión contiene tipo NULL
```

### Ejemplo 3: Variable No Declarada

**Entrada**:
```
int main ¿
    let int x$
    y = x + 5$
    return 0$
?
```

**Salida (error)**:
```
Error semantico: Variable y no declarada
Error semantico en asignacion (línea 3): Variable y no declarada
```

### Ejemplo 4: Precedencia de Operadores

**Entrada**:
```
int main ¿
    let int a$
    let int b$
    let int c$
    
    a = 5$
    b = 3$
    c = a + b * 2$
    
    return 0$
?
```

**Código 3D** (nota el orden: * antes que +):
```
a = null
b = null
t1 = b * null      (primero: multiplicación)
t2 = a + t1        (segundo: suma)
c = t2
```

---

## Referencias

### Archivos Involucrados

| Archivo | Propósito |
|---------|-----------|
| `programa/src/lexer.flex` | Definición del analizador léxico |
| `programa/src/sintax.cup` | Definición de la gramática y validaciones |
| `programa/src/Proye1_compi.java` | Punto de entrada principal |
| `programa/lib/jflex-full-1.9.1.jar` | Generador de léxer |
| `programa/lib/java-cup-11b.jar` | Generador de parser |

### Comandos de Compilación

```bash
# Regenerar léxer
java -jar programa/lib/jflex-full-1.9.1.jar programa/src/lexer.flex

# Regenerar parser
java -jar programa/lib/java-cup-11b.jar -expect 8 -destdir programa/src -parser Parser programa/src/sintax.cup

# Compilar Java
javac -cp "programa/lib/*;programa/src" programa/src/*.java

# Ejecutar
java -cp "programa/lib/*;programa/src" Proye1_compi [archivo]
```

---

## Conclusiones

El sistema de validaciones semánticas y generación de código 3D proporciona:

✅ **Verificación robusta de tipos**  
✅ **Detección temprana de errores**  
✅ **Generación de código intermedio funcional**  
✅ **Manejo apropiado de temporales**  
✅ **Exportación automatizada de resultados**  

El compilador está completamente operacional y listo para su uso en compilación de programas en el lenguaje definido.

---

**Versión**: 2.0  
**Última Actualización**: 1 de Diciembre de 2025  
**Estado**: ✅ DOCUMENTADO Y PROBADO
