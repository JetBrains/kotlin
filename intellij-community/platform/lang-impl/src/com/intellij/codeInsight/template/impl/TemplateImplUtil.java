// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.impl;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.hash.LinkedHashMap;

import java.util.LinkedHashSet;

/**
 * @author Maxim.Mossienko
 */
public class TemplateImplUtil {

  public static LinkedHashSet<String> parseVariableNames(CharSequence text) {
    LinkedHashSet<String> variableNames = new LinkedHashSet<>();
    TemplateTextLexer lexer = new TemplateTextLexer();
    lexer.start(text);

    while (true) {
      IElementType tokenType = lexer.getTokenType();
      if (tokenType == null) break;
      if (tokenType == TemplateTokenType.VARIABLE) {
        int start = lexer.getTokenStart();
        int end = lexer.getTokenEnd();
        String name = text.subSequence(start + 1, end - 1).toString();
        variableNames.add(name);
      }
      lexer.advance();
    }
    return variableNames;
  }

  public static LinkedHashMap<String, Variable> parseVariables(CharSequence text) {
    LinkedHashMap<String, Variable> variables = new LinkedHashMap<>();
    for (String name : parseVariableNames(text)) {
      variables.put(name, new Variable(name, "", "", true));
    }
    return variables;
  }

  public static boolean isValidVariableName(String varName) {
    return parseVariableNames("$" + varName + "$").contains(varName);
  }

  public static TextRange findVariableAtOffset(CharSequence text, int offset) {
    TemplateTextLexer lexer = new TemplateTextLexer();
    lexer.start(text);

    while (true) {
      IElementType tokenType = lexer.getTokenType();
      if (tokenType == null) {
        return null;
      }
      int start = lexer.getTokenStart();
      if (start < offset) {
        int end = lexer.getTokenEnd();
        if (end >= offset) {
          return tokenType == TemplateTokenType.VARIABLE ? new TextRange(start + 1, end - 1) : null;
        }
      }
      lexer.advance();
    }
  }
}
