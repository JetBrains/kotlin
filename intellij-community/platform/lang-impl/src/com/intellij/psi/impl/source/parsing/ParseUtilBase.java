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

package com.intellij.psi.impl.source.parsing;

import com.intellij.lang.ASTFactory;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerUtil;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.util.CharTable;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim.Mossienko
 */
public class ParseUtilBase {
  @Nullable
  public static TreeElement createTokenElement(Lexer lexer, CharTable table) {
    IElementType tokenType = lexer.getTokenType();
    if (tokenType == null) {
      return null;
    }
    else if (tokenType instanceof ILazyParseableElementType) {
      return ASTFactory.lazy((ILazyParseableElementType)tokenType, LexerUtil.internToken(lexer, table));
    }
    else {
      return ASTFactory.leaf(tokenType, LexerUtil.internToken(lexer, table));
    }
  }
}
