/* It's an automatically generated code. Do not modify it. */
package com.intellij.codeInsight.template.impl;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

%%

%{
   public _MacroLexer() {
     this((java.io.Reader)null);
   }
%}

%unicode
%class _MacroLexer
%implements FlexLexer
%function advance
%type IElementType

IDENTIFIER=[:jletter:] [:jletterdigit:]*
WHITE_SPACE_CHAR=[\ \n\r\t\f]
STRING_LITERAL=\"([^\\\"\r\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\[^\r\n]

%%

{IDENTIFIER} { return MacroTokenType.IDENTIFIER; }
{WHITE_SPACE_CHAR}+ { return MacroTokenType.WHITE_SPACE; }
{STRING_LITERAL} { return MacroTokenType.STRING_LITERAL; }
"(" { return MacroTokenType.LPAREN; }
")" { return MacroTokenType.RPAREN; }
"," { return MacroTokenType.COMMA; }
"=" { return MacroTokenType.EQ; }
[^] { return TokenType.BAD_CHARACTER; }
