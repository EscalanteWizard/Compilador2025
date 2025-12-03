# Compilador2025
Compilador imperativo desarrollado para el curso de Compiladores (Semestre 2 2025).

## Requisitos previos
- Java SE 8 o superior disponible en `PATH` para poder ejecutar `java` y `javac` desde PowerShell.
- Estar en la raíz del repositorio (`D:\Compilador2025`) y contar con las carpetas `programa/src`, `programa/lib`, `programa/input` y `programa/output`.

## Construcción desde cero
Ejecuta los comandos en PowerShell ubicándote en `D:\Compilador2025\programa` para generar el lexer, el parser y compilar todas las clases.

1. **Generar el lexer (JFlex)**
	```powershell
	java -jar lib\jflex-full-1.9.1.jar src\lexer.flex
	```

2. **Generar el parser y la tabla de símbolos (Java CUP)**
	```powershell
	java -jar lib\java-cup-11b.jar -destdir src -parser Parser src\sintax.cup
	```

3. **Compilar todas las clases Java**
	```powershell
	javac -cp "lib\*" src\*.java
	```

Los archivos resultantes (`Lexer.java`, `Parser.java`, `sym.java` y sus `.class`) quedarán en `programa/src`. Si alguno falta, repite los pasos anteriores.

## Ejecutar el análisis sobre `test.txt`
Desde la raíz (`D:\Compilador2025`) ejecuta la clase principal indicando explícitamente el archivo `programa\input\test.txt` para procesarlo:

```powershell
java -cp "programa\lib\*;programa\src" Proye1_compi programa\input\test.txt
```

La aplicación imprimirá los tokens en consola, guardará el listado completo en `programa/output/TOKENS.txt` y, tras el análisis sintáctico, escribirá `TablaSimbolos.txt` y `Codigo3D.txt` en la misma carpeta. Si omites el argumento, el programa utiliza `programa/input/test.txt` por defecto.

## Manual de usuario
1. **Preparar el entorno**
	- Instala un JDK 8+ y verifica con `java -version` y `javac -version`.
	- Clona o descarga el repositorio en `D:\Compilador2025` (puedes ubicarlo en otra ruta, pero usa rutas absolutas consistentes en los comandos).
	- Confirma que existan las carpetas `programa/lib`, `programa/src`, `programa/input` y `programa/output`. Si `output` no existe, se creará automáticamente al ejecutar el programa.

2. **Generar el lexer y parser**
	- Ubícate en `D:\Compilador2025\programa` y corre los comandos de las secciones anteriores (JFlex, CUP y luego `javac`).
	- Si cambias `lexer.flex` o `sintax.cup`, repite los pasos para regenerar las clases antes de compilar.

3. **Seleccionar el archivo de entrada**
	- Coloca tu programa fuente en `programa/input`. Hay ejemplos (`test.txt`, `test_simple.txt`, etc.) que puedes reutilizar o modificar.
	- Para conservar los ejemplos originales, crea un archivo nuevo (por ejemplo `programa/input/miprog.txt`).

4. **Ejecutar el compilador**
	- Desde la raíz ejecuta:
	  ```powershell
	  java -cp "programa\lib\*;programa\src" Proye1_compi programa\input\nombreArchivo.txt
	  ```
	- Si omites la ruta del archivo, se procesa `programa/input/test.txt` automáticamente.

5. **Revisar los resultados**
	- Consola: verás el listado de tokens con su línea y columna.
	- Carpeta `programa/output`:
	  - `TOKENS.txt`: mismo listado de tokens.
	  - `TablaSimbolos.txt`: tabla de símbolos globales y locales exportada por el parser.
	  - `Codigo3D.txt`: código de tres direcciones generado durante el análisis semántico.
	- Cada ejecución sobrescribe estos archivos; respáldalos si necesitas conservar versiones previas.

6. **Resolver errores comunes**
	- *No se encuentra `Lexer.class` o `Parser.class`*: regenera con JFlex/CUP y compila de nuevo.
	- *`Exception during parse`*: revisa la consola para ubicar la línea/columna del error sintáctico.
	- *`java.lang.NoClassDefFoundError`*: confirma que estás en la raíz del repositorio al ejecutar y que el classpath incluye `programa/lib/*;programa/src`.
	- *Encoding o caracteres especiales*: los archivos se esperan en UTF-8. Usa un editor que respete esa codificación para evitar tokens inesperados.
