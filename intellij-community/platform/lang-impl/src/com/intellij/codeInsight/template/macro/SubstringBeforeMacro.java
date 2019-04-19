// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubstringBeforeMacro extends MacroBase {
  public SubstringBeforeMacro() {
    super("substringBefore", "substringBefore(String, Delimiter)");
  }

  @Override
  protected Result calculateResult(@NotNull Expression[] params, ExpressionContext context, boolean quick) {
    if (params.length == 2) {
      String string = getTextResult(params[0], context);
      if (string == null) {
        return null;
      }
      String delimiter = getTextResult(params[1], context);
      if (delimiter == null) {
        return null;
      }
      int indexOf = string.indexOf(delimiter);
      return new TextResult(indexOf > 0 ? string.substring(0, indexOf) : "");
    }
    return null;
  }

  @Nullable
  private static String getTextResult(@NotNull Expression param, ExpressionContext context) {
    Result result = param.calculateResult(context);
    return result != null ? result.toString() : null;
  }
}
