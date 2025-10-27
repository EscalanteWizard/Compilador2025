/* JFlex example: partial Java language lexer specification */
import java_cup.runtime.*;

/**
 * This class is a simple example lexer.
 */
%%

%class Lexer
%unicode
%cup
%line
%column

%{
  StringBuffer string = new StringBuffer();

  private Symbol symbol(int type) {
    // attach current lexer line/column so CUP actions get left/right
    return new Symbol(type, yyline, yycolumn, null);
  }
  private Symbol symbol(int type, Object value) {
    // attach current lexer line/column and carry the value
    return new Symbol(type, yyline, yycolumn, value);
  }
  // helper when lexer wants to supply line/column info
  private Symbol symbol(int type, int left, int right) {
    // create a Symbol with location but no value
    return new Symbol(type, left, right, null);
  }

  private Symbol symbol(int type, int left, int right, Object value) {
    return new Symbol(type, left, right, value);
  }

  // expose current position and yytext to callers
  public int getLine() {
    return yyline;
  }

  public int getColumn() {
    return yycolumn;
  }

  public String getYYText() {
    return yytext();
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
// Comment can be the last line of the file, without line terminator.
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*

// Restrict identifiers to ASCII letters, digits and underscore (avoid including '$' in identifiers)
Identifier = [A-Za-z_][A-Za-z0-9_]*

DecIntegerLiteral = 0 | [1-9][0-9]*

digit = [0-9]
digitNoZero = [1-9]
dot = "\."
floatNum = ([0-9]*[.])?[0-9]+
true = "true"


%state STRING
%state CHARSTR

%%

/* keywords */
<YYINITIAL> "boolean"            { return symbol(sym.BOOL, yyline, yycolumn, yytext()); }
<YYINITIAL> "break"              { return symbol(sym.BREAK); }

<YYINITIAL> {dot}                { System.out.println("punto"); return symbol(sym.DOT, yyline, yycolumn, yytext()); }
<YYINITIAL> {DecIntegerLiteral}     { return symbol(sym.INT_LITERAL); }
<YYINITIAL> {floatNum}           { System.out.println("flotante"); return symbol(sym.FLOAT); }
<YYINITIAL> "true"               { System.out.println("true"); return symbol(sym.TRUE); }
<YYINITIAL> "false"              { return symbol(sym.FALSE); }

<YYINITIAL> "-"                  { return symbol(sym.MENOS); }
<YYINITIAL> "--"                 { return symbol(sym.DMENOS); }
<YYINITIAL> "+"                  { return symbol(sym.MAS); }
<YYINITIAL> "++"                 { return symbol(sym.DMAS); }

<YYINITIAL> "*"                  { return symbol(sym.MULTI); }
<YYINITIAL> "/"                  { return symbol(sym.DIV); }
<YYINITIAL> "//"                  { return symbol(sym.FLOAT_DIV); }
<YYINITIAL> "^"                  { return symbol(sym.POTENCIA); }

<YYINITIAL> "¿"                  { return symbol(sym.INIT_BLOCK); }
<YYINITIAL> "?"                  { return symbol(sym.END_BLOCK); }
<YYINITIAL> "$"                  { return symbol(sym.DOLLAR); }
<YYINITIAL> "global"               { return symbol(sym.GLOBAL); }

<YYINITIAL> "int"                { return symbol(sym.INT); }
<YYINITIAL> "char"               { return symbol(sym.CHAR); }
<YYINITIAL> "float"              { return symbol(sym.FLOAT); }
<YYINITIAL> "bool"               { return symbol(sym.BOOL); }
<YYINITIAL> "string"             { return symbol(sym.STRING); }
<YYINITIAL> "array"              { return symbol(sym.ARRAY); }

<YYINITIAL> "decide of"          { return symbol(sym.DECIDE_OF); }
<YYINITIAL> "->"                 { return symbol(sym.ARROW); }
<YYINITIAL> "else"               { return symbol(sym.ELSE); }
<YYINITIAL> "end decide"         { return symbol(sym.END_DECIDE); }

<YYINITIAL> "loop"               { return symbol(sym.LOOP); }
<YYINITIAL> "exit when"          { return symbol(sym.EXIT_WHEN); }
<YYINITIAL> "end loop"           { return symbol(sym.END_LOOP); }

<YYINITIAL> "for"                { return symbol(sym.FOR); }
<YYINITIAL> "to"                 { return symbol(sym.TO); }
<YYINITIAL> "downto"             { return symbol(sym.DOWNTO); }
<YYINITIAL> "step"               { return symbol(sym.STEP); }
<YYINITIAL> "do"                 { return symbol(sym.DO); }

<YYINITIAL> "("                   { return symbol(sym.PARENTS); }
<YYINITIAL> ")"                   { return symbol(sym.PARENTC); }

<YYINITIAL> "["                   { return symbol(sym.SQUARES); }
<YYINITIAL> "]"                   { return symbol(sym.SQUAREC); }

<YYINITIAL> "Є"                   { return symbol(sym.UKRA); }
<YYINITIAL> "Э"                   { return symbol(sym.RUSS); }

<YYINITIAL> "="                 { return symbol(sym.EQ); }
<YYINITIAL> ">"                  { return symbol(sym.GREATHER); }
<YYINITIAL> "<"                  { return symbol(sym.LOWER); }
<YYINITIAL> ">="                 { return symbol(sym.MORE); }
<YYINITIAL> "<="                 { return symbol(sym.LESS); }
<YYINITIAL> "=="                 { return symbol(sym.COMPARA); }
<YYINITIAL> "!="                 { return symbol(sym.DIFF); }
<YYINITIAL> "@"                 { return symbol(sym.AND); }
<YYINITIAL> "~"                 { return symbol(sym.OR); }
<YYINITIAL> "Σ"                  { return symbol(sym.NEGA); }

<YYINITIAL> "|"                  { return symbol(sym.COMMENT); }
<YYINITIAL> "!"                  { return symbol(sym.INIT_COMMENT); }
<YYINITIAL> "¡"                  { return symbol(sym.END_COMMENT); }

<YYINITIAL> "programa"                  { /* keyword 'programa' - ignored by parser grammar */ }
<YYINITIAL> "principal"                  { return symbol(sym.MAIN); }

<YYINITIAL> "func"               { return symbol(sym.FUNC); }
<YYINITIAL> "param"              { return symbol(sym.PARAM); }
<YYINITIAL> ","                  { return symbol(sym.COMA); }
<YYINITIAL> "let"                  { return symbol(sym.LET); }

<YYINITIAL> "return"             { return symbol(sym.RETURN); }
<YYINITIAL> "break"              { return symbol(sym.BREAK); }

<YYINITIAL> "input"              { return symbol(sym.INPUT); }
<YYINITIAL> "output"              { return symbol(sym.OUTPUT); }

<YYINITIAL> {
  /* identifiers */ 
  {Identifier}                   { return symbol(sym.IDENTIFIER, yytext()); }

  /* literals */
  <YYINITIAL> {DecIntegerLiteral}  { return symbol(sym.INT_LITERAL); }
  \"                             { string.setLength(0); yybegin(STRING); }
  \'                             { string.setLength(0); yybegin(CHARSTR); }

  /* operators */
  "="                            { return symbol(sym.EQ); }
  "=="                           { return symbol(sym.COMPARA); }
  "+"                            { return symbol(sym.MAS); }

  /* comments */
  {Comment}                      { /* ignore */ }

  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); 
                                   return symbol(sym.STRING_LITERAL, 
                                   string.toString()); }
  [^\n\r\"\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\"                           { string.append('\"'); }
  \\                             { string.append('\\'); }
}

<CHARSTR> {
  \'                             { yybegin(YYINITIAL); 
                                   return symbol(sym.CHARSTR,
                                   string.toString()); }
  [^\n\r\'\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\'                           { string.append('\''); }
  \\                             { string.append('\\'); }
}

/* Manejo de errores */
[^] { throw new RuntimeException("Carácter no válido: " + yytext()); }