// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors.highlighting;

import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HighlightsExtractor {
  private final Map<String,TextAttributesKey> myTags;
  private final Map<String,TextAttributesKey> myInlineElements;
  private final Map<String,ColorKey> myAdditionalColorKeyMap;
  private int myStartOffset;
  private int myEndOffset;

  private int mySkippedLen;
  private int myIndex;
  private boolean myIsOpeningTag;

  private final List<TextRange> mySkipped = new ArrayList<>();

  public HighlightsExtractor(@Nullable Map<String, TextAttributesKey> tags) {
    this(tags, null, null);
  }

  public HighlightsExtractor(@Nullable Map<String, TextAttributesKey> tags, @Nullable Map<String, TextAttributesKey> inlineElements,
                             @Nullable Map<String, ColorKey> additionalColorKeyMap) {
    myTags = tags;
    myInlineElements = inlineElements;
    myAdditionalColorKeyMap = additionalColorKeyMap;
  }

  public String extractHighlights(String text, List<HighlightData> highlights) {
    mySkipped.clear();
    if (ContainerUtil.isEmpty(myTags) && ContainerUtil.isEmpty(myInlineElements)) return text;
    resetIndices();
    Stack<HighlightData> highlightsStack = new Stack<>();
    while (true) {
      String tagName = findTagName(text);
      if (tagName == null || myIndex < 0) break;
      String tagNameWithoutParameters = StringUtil.substringBefore(tagName, " ");
      ColorKey additionalColorKey = myAdditionalColorKeyMap == null ? null 
                                                                    : myAdditionalColorKeyMap.get(tagNameWithoutParameters == null 
                                                                                                  ? tagName : tagNameWithoutParameters);
      if (myInlineElements != null && tagNameWithoutParameters != null && myInlineElements.containsKey(tagNameWithoutParameters)) {
        mySkippedLen += tagName.length() + 2;
        String hintText = tagName.substring(tagNameWithoutParameters.length()).trim();
        highlights.add(new InlineElementData(myStartOffset - mySkippedLen, myInlineElements.get(tagNameWithoutParameters), hintText,
                                             additionalColorKey));
      }
      else if (myTags != null && myTags.containsKey(tagName)) {
        if (myIsOpeningTag) {
          mySkippedLen += tagName.length() + 2;
          HighlightData highlightData = new HighlightData(myStartOffset - mySkippedLen, myTags.get(tagName), additionalColorKey);
          highlightsStack.push(highlightData);
        } else {
          HighlightData highlightData = highlightsStack.pop();
          highlightData.setEndOffset(myEndOffset - mySkippedLen);
          mySkippedLen += tagName.length() + 3;
          highlights.add(highlightData);
        }
      }
    }

    return cutDefinedTags(text);
  }

  private String findTagName(String text) {
    myIsOpeningTag = true;
    int openTag = text.indexOf('<', myIndex);
    if (openTag == -1) {
      return null;
    }
    while (text.charAt(openTag + 1) == '<') {
      openTag++;
    }
    if (text.charAt(openTag + 1) == '/') {
      myIsOpeningTag = false;
      openTag++;
    }
    if (!isValidTagFirstChar(text.charAt(openTag + 1))) {
      myIndex = openTag + 1;
      return "";
    }

    int closeTag = text.indexOf('>', openTag + 1);
    if (closeTag == -1) return null;
    int i = text.indexOf('<', openTag + 1);
    if (i != -1 && i < closeTag) {
      myIndex = i;
      return "";
    }
    final String tagName = text.substring(openTag + 1, closeTag);

    if (myIsOpeningTag) {
      myStartOffset = openTag + tagName.length() + 2;
      if (myTags != null && myTags.containsKey(tagName) ||
          myInlineElements != null && myInlineElements.containsKey(StringUtil.substringBefore(tagName, " "))) {
        mySkipped.add(TextRange.from(openTag, tagName.length() + 2));
      }
    } else {
      myEndOffset = openTag - 1;
      if (myTags != null && myTags.containsKey(tagName)) {
        mySkipped.add(TextRange.from(openTag - 1, tagName.length() + 3));
      }
    }
    myIndex = Math.max(myStartOffset, myEndOffset + 1);
    return tagName;
  }
  
  private static boolean isValidTagFirstChar(char c) {
    return Character.isLetter(c) || c == '_';
  }

  private String cutDefinedTags(String text) {
    StringBuilder builder = new StringBuilder(text);
    for (int i = mySkipped.size() - 1; i >= 0; i--) {
      TextRange range = mySkipped.get(i);
      builder.delete(range.getStartOffset(), range.getEndOffset());
    }
    return builder.toString();
  }

  private void resetIndices() {
    myIndex = 0;
    myStartOffset = 0;
    myEndOffset = 0;
    mySkippedLen = 0;
  }
}
