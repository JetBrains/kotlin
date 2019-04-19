/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.macro;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.NameUtil;
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

  @SuppressWarnings("StringToUpperCaseOrToLowerCaseWithoutLocale")
  @NotNull
  @VisibleForTesting
  public Result convertString(@NotNull String text) {
    final String[] strings = splitWords(text);
    if (strings.length > 0) {
      final StringBuilder buf = new StringBuilder();
      buf.append(strings[0].toLowerCase());
      for (int i = 1; i < strings.length; i++) {
        String string = strings[i];
        if (Character.isLetterOrDigit(string.charAt(0))) {
          buf.append(StringUtil.capitalize(string.toLowerCase()));
        }
      }
      return new TextResult(buf.toString());
    }
    return new TextResult("");
  }

  @NotNull
  protected String[] splitWords(String text) {
    return NameUtil.nameToWords(text);
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
