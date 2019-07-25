/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.intellij.codeInsight.template.impl.editorActions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TypedActionHandlerBase implements TypedActionHandlerEx {
  @Nullable protected final TypedActionHandler myOriginalHandler;

  public TypedActionHandlerBase(@Nullable TypedActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  public void beforeExecute(@NotNull Editor editor, char c, @NotNull DataContext context, @NotNull ActionPlan plan) {
    if (myOriginalHandler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)myOriginalHandler).beforeExecute(editor, c, context, plan);
    }
  }
}
