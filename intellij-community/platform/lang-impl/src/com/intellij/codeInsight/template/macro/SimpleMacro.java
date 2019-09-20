/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public abstract class SimpleMacro extends Macro {
  private final String myName;

  protected SimpleMacro(final String name) {
    myName = name;
  }

  @Override
  @NonNls
  public String getName() {
    return myName;
  }

  @Override
  public String getPresentableName() {
    return myName + "()";
  }

  @Override
  @NotNull
  @NonNls
  public String getDefaultValue() {
    return "11.11.1111";
  }

  @Override
  public Result calculateResult(@NotNull final Expression[] params, final ExpressionContext context) {
    return new TextResult(evaluateSimpleMacro(params, context));
  }

  @Override
  public Result calculateQuickResult(@NotNull final Expression[] params, final ExpressionContext context) {
    return calculateResult(params, context);
  }

  protected abstract String evaluateSimpleMacro(Expression[] params, final ExpressionContext context);
}
