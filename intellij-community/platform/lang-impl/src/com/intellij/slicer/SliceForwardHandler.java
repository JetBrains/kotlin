/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import javax.swing.*;
import java.util.List;

/**
 * @author cdr
 */
public class SliceForwardHandler extends SliceHandler {
  public SliceForwardHandler() {
    super(false);
  }

  @Override
  public SliceAnalysisParams askForParams(PsiElement element, boolean dataFlowToThis, SliceManager.StoredSettingsBean storedSettingsBean, String dialogTitle) {
    AnalysisScope analysisScope = new AnalysisScope(element.getContainingFile());
    Module module = ModuleUtilCore.findModuleForPsiElement(element);

    Project myProject = element.getProject();
    final SliceForwardForm form = new SliceForwardForm();
    form.init(storedSettingsBean.showDereferences);

    AnalysisUIOptions analysisUIOptions = new AnalysisUIOptions();
    analysisUIOptions.loadState(storedSettingsBean.analysisUIOptions);

    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(myProject, analysisScope, module, element);
    BaseAnalysisActionDialog dialog = new BaseAnalysisActionDialog(dialogTitle, "Analyze scope", myProject,
                                                                   items, analysisUIOptions, true) {
      @Override
      protected JComponent getAdditionalActionSettings(Project project) {
        return form.getComponent();
      }
    };
    if (!dialog.showAndGet()) {
      return null;
    }

    storedSettingsBean.analysisUIOptions.loadState(analysisUIOptions);
    storedSettingsBean.showDereferences = form.isToShowDerefs();

    AnalysisScope scope = dialog.getScope(analysisUIOptions, analysisScope, myProject, module);

    SliceAnalysisParams params = new SliceAnalysisParams();
    params.scope = scope;
    params.dataFlowToThis = dataFlowToThis;
    params.showInstanceDereferences = form.isToShowDerefs();
    return params;
  }
}