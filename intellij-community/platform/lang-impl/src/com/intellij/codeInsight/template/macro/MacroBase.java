/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.codeInsight.template.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class MacroBase extends Macro {
  private final String myName;
  private final String myDescription;

  public MacroBase(String name, String description) {
    myName = name;
    myDescription = description;
  }

  @Nullable
  protected abstract Result calculateResult(@NotNull Expression[] params, ExpressionContext context, boolean quick);

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    return calculateResult(params, context, false);
  }

  @Override
  public Result calculateQuickResult(@NotNull Expression[] params, ExpressionContext context) {
    return calculateResult(params, context, true);
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getPresentableName() {
    return myDescription;
  }

  @NotNull
  @Override
  public String getDefaultValue() {
    return "a";
  }

  @Nullable
  public static String getTextResult(@NotNull Expression[] params, final ExpressionContext context) {
    return getTextResult(params, context, false);
  }

  @Nullable
  public static String getTextResult(@NotNull Expression[] params, final ExpressionContext context, boolean useSelection) {
    if (params.length == 1) {
      Result result = params[0].calculateResult(context);
      if (result == null && useSelection) {
        final String property = context.getProperty(ExpressionContext.SELECTION);
        if (property != null) {
          result = new TextResult(property);
        }
      }
      return result == null ? null : result.toString();
    }
    return null;
  }
}
