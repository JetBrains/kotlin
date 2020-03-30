// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

  protected FormatterTag getFormatterTag(@NotNull PsiComment comment) {
    CharSequence nodeChars = comment.getNode().getChars();
    return extractFormatterTag(nodeChars, 0, nodeChars.length());
  }

  private FormatterTag extractFormatterTag(@NotNull CharSequence chars, int startOffset, int endOffset) {
    if (mySettings.FORMATTER_TAGS_ACCEPT_REGEXP) {
      Pattern onPattern = mySettings.getFormatterOnPattern();
      Pattern offPattern = mySettings.getFormatterOffPattern();
      if (onPattern != null && onPattern.matcher(chars.subSequence(startOffset, endOffset)).find()) return FormatterTag.ON;
      if (offPattern != null && offPattern.matcher(chars.subSequence(startOffset, endOffset)).find()) return FormatterTag.OFF;
    }
    else {
      for (int i = startOffset; i < endOffset; i++) {
        if (isFormatterTagAt(chars, i, mySettings.FORMATTER_ON_TAG)) return FormatterTag.ON;
        if (isFormatterTagAt(chars, i, mySettings.FORMATTER_OFF_TAG)) return FormatterTag.OFF;
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
    if (!mySettings.FORMATTER_TAGS_ENABLED) {
      return Collections.singletonList(initialRange);
    }
    EnabledRangesCollector collector = new EnabledRangesCollector(initialRange);
    collector.processText(rootNode.getChars());
    return collector.getRanges();
  }

  private class EnabledRangesCollector {
    private final List<FormatterTagInfo> myTagInfoList = new ArrayList<>();
    private final TextRange myInitialRange;

    private EnabledRangesCollector(TextRange initialRange) {
      myInitialRange = initialRange;
    }

    private void processText(@NotNull CharSequence chars) {
      int lineStart = 0;
      for (int currPos = 0; currPos < chars.length(); currPos ++) {
        char c = chars.charAt(currPos);
        if (c == '\n') {
          FormatterTag formatterTag = extractFormatterTag(chars, lineStart, currPos);
          //noinspection EnumSwitchStatementWhichMissesCases
          switch (formatterTag) {
            case OFF:
              myTagInfoList.add(
                new FormatterTagInfo(lineStart, FormatterTag.OFF));
              break;
            case ON:
              myTagInfoList.add(
                new FormatterTagInfo(lineStart, FormatterTag.ON));
              break;
          }
          lineStart = currPos + 1;
        }
      }
    }

    private List<TextRange> getRanges() {
      List<TextRange> enabledRanges = new ArrayList<>();
      myTagInfoList.sort(Comparator.comparingInt(info -> info.offset));

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
