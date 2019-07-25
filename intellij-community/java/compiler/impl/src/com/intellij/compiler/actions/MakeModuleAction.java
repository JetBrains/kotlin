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
package com.intellij.compiler.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;

public class MakeModuleAction extends CompileActionBase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.actions.MakeModuleAction");

  @Override
  protected void doAction(DataContext dataContext, Project project) {
    Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    Module module;
    if (modules == null) {
      module = LangDataKeys.MODULE.getData(dataContext);
      if (module == null) {
        return;
      }
      modules = new Module[]{module};
    }
    try {
      ProjectTaskManager.getInstance(project).build(modules);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event){
    super.update(event);
    Presentation presentation = event.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }
    final DataContext dataContext = event.getDataContext();
    final Module module = LangDataKeys.MODULE.getData(dataContext);
    Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    final boolean isEnabled = module != null || modules != null;
    presentation.setEnabled(isEnabled);
    final String actionName = getTemplatePresentation().getTextWithMnemonic();

    String presentationText;
    if (modules != null) {
      String text = actionName;
      for (int i = 0; i < modules.length; i++) {
        if (text.length() > 30) {
          text = CompilerBundle.message("action.make.selected.modules.text");
          break;
        }
        Module toMake = modules[i];
        if (i!=0) {
          text += ",";
        }
        text += " '" + toMake.getName() + "'";
      }
      presentationText = text;
    }
    else if (module != null) {
      presentationText = actionName + " '" + module.getName() + "'";
    }
    else {
      presentationText = actionName;
    }
    presentation.setText(presentationText);
    presentation.setVisible(isEnabled || !ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace()));
  }
}
