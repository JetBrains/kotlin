/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.template.impl;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.macro.MacroFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@VisibleForTesting
public class MacroParser {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.template.impl.MacroParser");
  
  @NotNull
  public static Expression parse(@Nullable String expression) {
    if (StringUtil.isEmpty(expression)) {
      return new ConstantNode("");
    }
    Lexer lexer = new MacroLexer();
    lexer.start(expression);
    skipWhitespaces(lexer);
    return parseMacro(lexer, expression);
  }

  //-----------------------------------------------------------------------------------
  private static void advance(Lexer lexer) {
    lexer.advance();
    skipWhitespaces(lexer);
  }

  //-----------------------------------------------------------------------------------
  private static void skipWhitespaces(Lexer lexer) {
    while (lexer.getTokenType() == MacroTokenType.WHITE_SPACE) {
      lexer.advance();
    }
  }

  //-----------------------------------------------------------------------------------
  private static String getString(Lexer lexer, String expression) {
    return expression.substring(lexer.getTokenStart(), lexer.getTokenEnd());
  }

  //-----------------------------------------------------------------------------------
  @SuppressWarnings({"HardCodedStringLiteral"})
  private static Expression parseMacro(Lexer lexer, String expression) {
    IElementType tokenType = lexer.getTokenType();
    String token = getString(lexer, expression);
    if (tokenType == MacroTokenType.STRING_LITERAL) {
      advance(lexer);
      return new ConstantNode(parseStringLiteral(token));
    }

    if (tokenType != MacroTokenType.IDENTIFIER) {
      LOG.info("Bad macro syntax: Not identifier: " + token);
      advance(lexer);
      return new ConstantNode("");
    }

    List<Macro> macros = MacroFactory.getMacros(token);
    if (macros.isEmpty()) {
      return parseVariable(lexer, expression);
    }

    advance(lexer);
    MacroCallNode macroCallNode = new MacroCallNode(macros.get(0));
    if (lexer.getTokenType() == null) {
      return macroCallNode;
    }

    if (lexer.getTokenType() != MacroTokenType.LPAREN) {
      return macroCallNode;
    }

    advance(lexer);
    parseParameters(macroCallNode, lexer, expression);
    if (lexer.getTokenType() != MacroTokenType.RPAREN) {
      LOG.info("Bad macro syntax: ) expected: " + expression);
    }
    advance(lexer);
    return macroCallNode;
  }

  private static String parseStringLiteral(String token) {
    StringBuilder sb = new StringBuilder(token.length() - 2);
    int i = 1;
    while (i < token.length() - 1) {
      char c = token.charAt(i);
      if (c == '\\') {
        c = token.charAt(++i);
        if (c == 'n') sb.append('\n');
        else if (c == 't') sb.append('\t');
        else if (c == 'f') sb.append('\f');
        else sb.append(c);
      } else {
        sb.append(c);
      }
      i++;
    }
    return sb.toString();
  }

  private static void parseParameters(MacroCallNode macroCallNode, Lexer lexer, String expression) {
    if (lexer.getTokenType() != MacroTokenType.RPAREN) {
      while (lexer.getTokenType() != null) {
        Expression node = parseMacro(lexer, expression);
        macroCallNode.addParameter(node);

        if (lexer.getTokenType() == MacroTokenType.COMMA) {
          advance(lexer);
        }
        else {
          break;
        }
      }
    }
  }

  private static Expression parseVariable(Lexer lexer, String expression) {
    String variableName = getString(lexer, expression);
    advance(lexer);

    if (lexer.getTokenType() == null) {
      if (TemplateImpl.END.equals(variableName)) {
        return new EmptyNode();
      }

      return new VariableNode(variableName, null);
    }

    if (lexer.getTokenType() != MacroTokenType.EQ) {
      return new VariableNode(variableName, null);
    }

    advance(lexer);
    Expression node = parseMacro(lexer, expression);
    return new VariableNode(variableName, node);
  }
}
