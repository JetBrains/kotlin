/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.engine;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.AbstractBlockWrapper;
import com.intellij.formatting.Block;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

public class BlockIndentOptions {
  private final CodeStyleSettings mySettings;
  private final CommonCodeStyleSettings.IndentOptions myIndentOptions;
  private final int myRightMargin;

  public BlockIndentOptions(@NotNull CodeStyleSettings settings, @NotNull CommonCodeStyleSettings.IndentOptions indentOptions, Block block) {
    mySettings = settings;
    myIndentOptions = indentOptions;
    myRightMargin = calcRightMargin(block);
  }
  
  public CommonCodeStyleSettings.IndentOptions getIndentOptions() {
    return myIndentOptions;
  }

  @NotNull
  public CommonCodeStyleSettings.IndentOptions getIndentOptions(@NotNull AbstractBlockWrapper block) {
    if (myIndentOptions.isOverrideLanguageOptions()) return myIndentOptions;

    final Language language = block.getLanguage();
    if (language != null) {
      final CommonCodeStyleSettings commonSettings = mySettings.getCommonSettings(language);
      if (commonSettings != null) {
        final CommonCodeStyleSettings.IndentOptions result = commonSettings.getIndentOptions();
        if (result != null) {
          return result;
        }
      }
    }

    return myIndentOptions;
  }
  
  public int getRightMargin() {
    return myRightMargin;
  }
  
  private int calcRightMargin(Block rootBlock) {
    Language language = null;
    if (rootBlock instanceof ASTBlock) {
      ASTNode node = ((ASTBlock)rootBlock).getNode();
      if (node != null) {
        PsiElement psiElement = node.getPsi();
        if (psiElement.isValid()) {
          PsiFile psiFile = psiElement.getContainingFile();
          if (psiFile != null) {
            language = psiFile.getViewProvider().getBaseLanguage();
          }
        }
      }
    }
    return mySettings.getRightMargin(language);
  }
}