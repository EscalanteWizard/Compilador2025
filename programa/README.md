# Generación rápida de artefactos (JFlex + Java CUP)

Esta guía mínima muestra los comandos para generar los archivos del lexer y parser y compilarlos para obtener los `.class`.

Ejecuta los comandos desde `D:\Compilador2025\programa` (PowerShell):

1) Generar `Lexer.java` con JFlex

```powershell
java -jar lib\jflex-full-1.9.1.jar src\lexer.flex
```

Resultado esperado: `src\Lexer.java`. JFlex puede crear además un archivo de respaldo `src\Lexer.java~`.

2) Generar `Parser.java` y `sym.java` con Java CUP

```powershell
java -jar lib\java-cup-11b.jar -destdir src -parser Parser src\sintax.cup
```

Resultado esperado: `src\Parser.java` y `src\sym.java`.

3) Compilar los `.java` para obtener las clases (`.class`)

```powershell
javac -cp "lib\*" src\*.java
```

Resultados finales esperados en `src\`: `Lexer.java`, `Lexer.java~` (backup, opcional), `Lexer.class`, `Parser.java`, `Parser.class`, `sym.java`, `sym.class`.

Si alguno de esos archivos no aparece, regenera (pasos 1 y 2) y recompila (paso 3). Asegúrate también de que los nombres de token en `lexer.flex` coincidan exactamente con los terminales en `sintax.cup`.

4) Ejecuta la clase principal Proye1_compi. 
Esa clase:
hace el análisis léxico y va imprimiendo los tokens en la consola,
además escribe los tokens en TOKENS.txt,
luego lanza el parser (puede mostrar errores de sintaxis en stderr).

    # desde D:\Compilador2025
    java -cp "programa\lib\*;programa\src" Proye1_compi