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

package com.intellij.codeInsight.template.impl;

import com.intellij.lang.Language;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

/**
 * @author yole
 */
public interface MacroTokenType {
  IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
  IElementType IDENTIFIER = new IElementType("IDENTIFIER", Language.ANY);
  IElementType STRING_LITERAL = new IElementType("STRING_LITERAL", Language.ANY);
  IElementType LPAREN = new IElementType("LPAREN", Language.ANY);
  IElementType RPAREN = new IElementType("RPAREN", Language.ANY);
  IElementType COMMA = new IElementType("COMMA", Language.ANY);
  IElementType EQ = new IElementType("EQ", Language.ANY);
}
