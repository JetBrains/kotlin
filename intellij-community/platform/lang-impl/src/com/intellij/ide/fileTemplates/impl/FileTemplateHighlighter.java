// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.impl;

import com.intellij.codeInsight.template.impl.TemplateColors;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class FileTemplateHighlighter extends SyntaxHighlighterBase {
  private final Lexer myLexer;

  public FileTemplateHighlighter() {
    myLexer = FileTemplateConfigurable.createDefaultLexer();
  }

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return myLexer;
  }

  @Override
  @NotNull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    if (tokenType == FileTemplateTokenType.MACRO || tokenType == FileTemplateTokenType.DIRECTIVE) {
      return pack(TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
    }

    return TextAttributesKey.EMPTY_ARRAY;
  }
}