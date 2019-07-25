/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.formatting;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.InjectedLanguageBlockWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Rustam Vishnyakov
 */
public class FormatterTagHandler {

  public enum FormatterTag {ON, OFF, NONE}
  private final CodeStyleSettings mySettings;

  public FormatterTagHandler(CodeStyleSettings settings) {
    mySettings = settings;
  }

  public FormatterTag getFormatterTag(Block block) {
    if (mySettings.FORMATTER_TAGS_ENABLED &&
        !StringUtil.isEmpty(mySettings.FORMATTER_ON_TAG) &&
        !StringUtil.isEmpty(mySettings.FORMATTER_OFF_TAG)) {
      if (block instanceof ASTBlock) {
        ASTNode node = ((ASTBlock)block).getNode();
        if (node != null) {
          PsiElement element = node.getPsi();
          if (element instanceof PsiComment) {
            return getFormatterTag((PsiComment)element);
          }
        }
      }
      else if (block instanceof InjectedLanguageBlockWrapper) {
        return getFormatterTag(((InjectedLanguageBlockWrapper)block).getOriginal());
      }
    }
    return FormatterTag.NONE;
  }

  protected FormatterTag getFormatterTag(@NotNull PsiComment comment) {
    CharSequence nodeChars = comment.getNode().getChars();
    if (mySettings.FORMATTER_TAGS_ACCEPT_REGEXP) {
      Pattern onPattern = mySettings.getFormatterOnPattern();
      Pattern offPattern = mySettings.getFormatterOffPattern();
      if (onPattern != null && onPattern.matcher(nodeChars).find()) return FormatterTag.ON;
      if (offPattern != null && offPattern.matcher(nodeChars).find()) return FormatterTag.OFF;
    }
    else {
      for (int i = 0; i < nodeChars.length(); i++) {
        if (isFormatterTagAt(nodeChars, i, mySettings.FORMATTER_ON_TAG)) return FormatterTag.ON;
        if (isFormatterTagAt(nodeChars, i, mySettings.FORMATTER_OFF_TAG)) return FormatterTag.OFF;
      }
    }
    return FormatterTag.NONE;
  }

  private static boolean isFormatterTagAt(@NotNull CharSequence s, int pos, @NotNull String tagName) {
    if (!tagName.isEmpty() && tagName.charAt(0) == s.charAt(pos)) {
      int end = pos + tagName.length();
      if (end <= s.length()) {
        return StringUtil.equalsIgnoreCase(s.subSequence(pos, end), tagName);
      }
    }
    return false;
  }

  public List<TextRange> getEnabledRanges(ASTNode rootNode, TextRange initialRange) {
    EnabledRangesCollector collector = new EnabledRangesCollector(initialRange);
    rootNode.getPsi().accept(collector);
    return collector.getRanges();
  }

  private class EnabledRangesCollector extends PsiRecursiveElementVisitor {
    private final List<FormatterTagInfo> myTagInfoList = new ArrayList<>();
    private final TextRange myInitialRange;

    private EnabledRangesCollector(TextRange initialRange) {
      myInitialRange = initialRange;
    }

    @Override
    public void visitComment(PsiComment comment) {
      FormatterTag tag = getFormatterTag(comment);
      //noinspection EnumSwitchStatementWhichMissesCases
      switch (tag) {
        case OFF:
          myTagInfoList.add(new FormatterTagInfo(comment.getTextRange().getEndOffset(), FormatterTag.OFF));
          break;
        case ON:
          myTagInfoList.add(new FormatterTagInfo(comment.getTextRange().getEndOffset(), FormatterTag.ON));
          break;
      }
    }

    private List<TextRange> getRanges() {
      List<TextRange> enabledRanges = new ArrayList<>();
      Collections.sort(myTagInfoList, (tagInfo1, tagInfo2) -> tagInfo1.offset - tagInfo2.offset);

      int start = myInitialRange.getStartOffset();
      boolean formatterEnabled = true;
      for (FormatterTagInfo tagInfo: myTagInfoList) {
        if (tagInfo.tag == FormatterTag.OFF && formatterEnabled) {
          if (tagInfo.offset > start) {
            TextRange range = new TextRange(start, tagInfo.offset);
            enabledRanges.add(range);
          }
          formatterEnabled = false;
        }
        else if (tagInfo.tag == FormatterTag.ON && !formatterEnabled) {
          start = Math.max(tagInfo.offset, myInitialRange.getStartOffset());
          if (start >= myInitialRange.getEndOffset()) break;
          formatterEnabled = true;
        }
      }
      if (start < myInitialRange.getEndOffset() && formatterEnabled) {
        enabledRanges.add(new TextRange(start, myInitialRange.getEndOffset()));
      }
      return enabledRanges;
    }

    private class FormatterTagInfo {
      public int offset;
      public FormatterTag tag;

      private FormatterTagInfo(int offset, FormatterTag tag) {
        this.offset = offset;
        this.tag = tag;
      }
    }
  }
}
