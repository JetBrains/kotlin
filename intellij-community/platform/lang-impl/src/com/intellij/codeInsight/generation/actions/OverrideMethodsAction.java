/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.OverrideMethodsHandler;
import com.intellij.lang.CodeInsightActions;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.lang.LanguageExtension;
import org.jetbrains.annotations.NotNull;

public class OverrideMethodsAction extends PresentableActionHandlerBasedAction {

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new OverrideMethodsHandler();
  }

  @NotNull
  @Override
  protected LanguageExtension<LanguageCodeInsightActionHandler> getLanguageExtension() {
    return CodeInsightActions.OVERRIDE_METHOD;
  }
}