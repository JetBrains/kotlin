// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
public final class ParseUtilBase {
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
