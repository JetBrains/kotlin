// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;

public class SelectionNode extends Expression {
  public SelectionNode() {
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    return LookupElement.EMPTY_ARRAY;
  }

  @Override
  public Result calculateQuickResult(ExpressionContext context) {
    final String selection = context.getProperty(ExpressionContext.SELECTION);
    return new TextResult(selection == null ? "" : selection);
  }

  @Override
  public Result calculateResult(ExpressionContext context) {
    return calculateQuickResult(context);
  }

  @Override
  public boolean requiresCommittedPSI() {
    return false;
  }
}
