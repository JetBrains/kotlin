// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;

/**
 * @author peter
 */
public class LiveTemplateCharFilter extends CharFilter {
  @Override
  public Result acceptChar(char c, int prefixLength, Lookup lookup) {
    LookupElement item = lookup.getCurrentItem();
    if (item instanceof LiveTemplateLookupElement && lookup.isCompletion()) {
      if (Character.isJavaIdentifierPart(c) || c == '%') return Result.ADD_TO_PREFIX;

      if (c == ((LiveTemplateLookupElement)item).getTemplateShortcut()) {
        return Result.SELECT_ITEM_AND_FINISH_LOOKUP;
      }
      return Result.HIDE_LOOKUP;
    }
    if (item instanceof TemplateExpressionLookupElement) {
      if (Character.isJavaIdentifierPart(c)) return Result.ADD_TO_PREFIX;
      if (CodeInsightSettings.getInstance().isSelectAutopopupSuggestionsByChars()) {
        return null;
      }
      return Result.HIDE_LOOKUP;
    }

    return null;
  }
}
