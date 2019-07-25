/* It's an automatically generated code. Do not modify it. */
package com.intellij.psi.search.scope.packageSet.lexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

%%

%{
  public _ScopesLexer() {
    this((java.io.Reader)null);
  }
%}

%unicode
%class _ScopesLexer
%implements FlexLexer
%function advance
%type IElementType

IDENTIFIER=[:jletter:] [:jletterdigit:]*
WHITE_SPACE_CHAR=[\ \n\r\t\f]
DIGIT=[0-9]+

%%
<YYINITIAL> {IDENTIFIER} { return ScopeTokenTypes.IDENTIFIER; }
<YYINITIAL> {WHITE_SPACE_CHAR}+ { return ScopeTokenTypes.WHITE_SPACE; }
<YYINITIAL> {DIGIT}+ { return ScopeTokenTypes.INTEGER_LITERAL; }
<YYINITIAL> "||" { return ScopeTokenTypes.OROR; }
<YYINITIAL> "&&" { return ScopeTokenTypes.ANDAND; }
<YYINITIAL> "!" { return ScopeTokenTypes.EXCL; }
<YYINITIAL> "$" { return ScopeTokenTypes.IDENTIFIER;}
<YYINITIAL> "-" { return ScopeTokenTypes.MINUS;}
<YYINITIAL> "~" { return ScopeTokenTypes.TILDE;}

<YYINITIAL> "["   { return ScopeTokenTypes.LBRACKET; }
<YYINITIAL> "]"   { return ScopeTokenTypes.RBRACKET; }
<YYINITIAL> "("   { return ScopeTokenTypes.LPARENTH; }
<YYINITIAL> ")"   { return ScopeTokenTypes.RPARENTH; }
<YYINITIAL> "."   { return ScopeTokenTypes.DOT; }

<YYINITIAL> ":" { return ScopeTokenTypes.COLON; }
<YYINITIAL> "*" { return ScopeTokenTypes.ASTERISK; }
<YYINITIAL> "/" { return ScopeTokenTypes.DIV; }

<YYINITIAL> "#" { return ScopeTokenTypes.SHARP; }
<YYINITIAL> [^] { return ScopeTokenTypes.BAD_CHARACTER; }

