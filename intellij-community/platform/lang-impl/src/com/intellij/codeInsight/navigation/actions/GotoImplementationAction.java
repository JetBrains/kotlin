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

package com.intellij.codeInsight.navigation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import org.jetbrains.annotations.NotNull;

public class GotoImplementationAction extends BaseCodeInsightAction {
  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler(){
    return new GotoImplementationHandler();
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  public void update(@NotNull final AnActionEvent event) {
    if (!DefinitionsScopedSearch.INSTANCE.hasAnyExecutors()) {
      event.getPresentation().setVisible(false);
    }
    else {
      super.update(event);
    }
  }
}
