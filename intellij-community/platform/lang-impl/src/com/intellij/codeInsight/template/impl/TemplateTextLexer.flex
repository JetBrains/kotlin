/* It's an automatically generated code. Do not modify it. */
package com.intellij.codeInsight.template.impl;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%{
   public _TemplateTextLexer() {
     this((java.io.Reader)null);
   }
%}

%unicode
%class _TemplateTextLexer
%implements FlexLexer
%function advance
%type IElementType

ALPHA=[A-Za-z_]
DIGIT=[0-9]
VARIABLE="$"({ALPHA}|{DIGIT})+"$"

%%

<YYINITIAL> "$""$" { return TemplateTokenType.ESCAPE_DOLLAR; }
<YYINITIAL> {VARIABLE} { return TemplateTokenType.VARIABLE; }
<YYINITIAL> [^] { return TemplateTokenType.TEXT; }
