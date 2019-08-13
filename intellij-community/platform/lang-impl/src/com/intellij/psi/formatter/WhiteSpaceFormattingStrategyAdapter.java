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
package com.intellij.psi.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class WhiteSpaceFormattingStrategyAdapter implements WhiteSpaceFormattingStrategy {
  
  private final WhiteSpaceFormattingStrategy DELEGATE = new StaticSymbolWhiteSpaceDefinitionStrategy(' ', '\t', '\n');
  
  @Override
  public int check(@NotNull CharSequence text, int start, int end) {
    return DELEGATE.check(text, start, end);
  }

  @Override
  public boolean containsWhitespacesOnly(@NotNull ASTNode node) {
    return false;
  }

  @Override
  public boolean replaceDefaultStrategy() {
    return false;
  }

  @NotNull
  @Override
  public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText,
                                                  @NotNull CharSequence text,
                                                  int startOffset,
                                                  int endOffset, CodeStyleSettings codeStyleSettings, ASTNode nodeAfter) {
    return whiteSpaceText;
  }

  @Override
  public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText,
                                                  @NotNull PsiElement startElement,
                                                  int startOffset,
                                                  int endOffset, CodeStyleSettings codeStyleSettings) {
    return whiteSpaceText;
  }

  @Override
  public boolean addWhitespace(@NotNull ASTNode treePrev, @NotNull LeafElement whiteSpaceElement) {
    return false;
  }
}
