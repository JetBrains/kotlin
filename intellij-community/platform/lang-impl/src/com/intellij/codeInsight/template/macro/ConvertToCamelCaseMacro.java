// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.macro;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.text.NameUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class ConvertToCamelCaseMacro extends MacroBase {

  public ConvertToCamelCaseMacro() {
    super("camelCase", "camelCase(String)");
  }

  private ConvertToCamelCaseMacro(String name, String description) {
    super(name, description);
  }

  @Nullable
  @Override
  protected Result calculateResult(@NotNull Expression[] params, ExpressionContext context, boolean quick) {
    final String text = getTextResult(params, context, true);
    return text != null ? convertString(text) : null;
  }

  @NotNull
  @VisibleForTesting
  public Result convertString(@NotNull String text) {
    final String[] strings = splitWords(text);
    if (strings.length > 0) {
      final StringBuilder buf = new StringBuilder();
      buf.append(StringUtil.toLowerCase(strings[0]));
      for (int i = 1; i < strings.length; i++) {
        String string = strings[i];
        if (Character.isLetterOrDigit(string.charAt(0))) {
          buf.append(StringUtil.capitalize(StringUtil.toLowerCase(string)));
        }
      }
      return new TextResult(buf.toString());
    }
    return new TextResult("");
  }

  @NotNull
  protected String[] splitWords(String text) {
    return NameUtilCore.nameToWords(text);
  }

  public static class ReplaceUnderscoresToCamelCaseMacro extends ConvertToCamelCaseMacro {
    public ReplaceUnderscoresToCamelCaseMacro() {
      super("underscoresToCamelCase", CodeInsightBundle.message("macro.undescoresToCamelCase.string"));
    }

    @NotNull
    @Override
    protected String[] splitWords(String text) {
      return text.split("_");
    }
  }
}
