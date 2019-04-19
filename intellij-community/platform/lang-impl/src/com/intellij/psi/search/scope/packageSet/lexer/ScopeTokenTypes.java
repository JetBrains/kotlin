/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.psi.search.scope.packageSet.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public interface ScopeTokenTypes extends TokenType {
  IElementType IDENTIFIER = new ScopeTokenType("Scope.IDENTIFIER");
  IElementType INTEGER_LITERAL = new ScopeTokenType("Scope.INTEGER_LITERAL");
  IElementType OROR = new ScopeTokenType("Scope.OROR");
  IElementType ANDAND = new ScopeTokenType("Scope.ANDAND");
  IElementType EXCL = new ScopeTokenType("Scope.EXCL");
  IElementType MINUS = new ScopeTokenType("Scope.MINUS");
  IElementType LBRACKET = new ScopeTokenType("Scope.LBRACKET");
  IElementType RBRACKET = new ScopeTokenType("Scope.RBRACKET");
  IElementType LPARENTH = new ScopeTokenType("Scope.LPARENTH");
  IElementType RPARENTH = new ScopeTokenType("Scope.RPARENTH");
  IElementType TILDE = new ScopeTokenType("TILDE");
  IElementType DOT = new ScopeTokenType("Scope.DOT");
  IElementType COLON = new ScopeTokenType("Scope.COLON");
  IElementType ASTERISK = new ScopeTokenType("Scope.ASTERISK");
  IElementType DIV = new ScopeTokenType("Scope.DIV");
  IElementType SHARP = new ScopeTokenType("Scope.SHARP");
}