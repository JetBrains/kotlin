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

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.*;
import org.jetbrains.annotations.NotNull;

public class LineNumberMacro extends Macro {
  @Override
  public String getName() {
    return "lineNumber";
  }

  @Override
  public String getPresentableName() {
    return CodeInsightBundle.message("macro.linenumber");
  }

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    final int offset = context.getStartOffset();
    int line = context.getEditor().offsetToLogicalPosition(offset).line + 1;
    return new TextResult("" + line);
  }

  @Override
  public Result calculateQuickResult(@NotNull Expression[] params, ExpressionContext context) {
    return calculateResult(params, context);
  }

}