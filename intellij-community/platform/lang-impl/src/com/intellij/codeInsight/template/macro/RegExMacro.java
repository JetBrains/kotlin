// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RegExMacro extends MacroBase {

  public RegExMacro() {
    super("regularExpression", "regularExpression(String, Pattern, Replacement)");
  }

  @Nullable
  @Override
  protected Result calculateResult(@NotNull Expression[] params, ExpressionContext context, boolean quick) {
    if (params.length != 3) {
      return null;
    }
    Result value = params[0].calculateResult(context);
    if (value == null) {
      return null;
    }
    Result pattern = params[1].calculateResult(context);
    if (pattern == null) {
      return null;
    }
    Result replacement = params[2].calculateResult(context);
    if (replacement == null) {
      return null;
    }
    return new TextResult(value.toString().replaceAll(pattern.toString(), replacement.toString()));
  }
}
