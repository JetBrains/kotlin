// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;

class SliceBackwardHandler extends SliceHandler {
  SliceBackwardHandler() {
    super(true);
  }

  @Override
  public SliceAnalysisParams askForParams(PsiElement element,
                                          SliceManager.StoredSettingsBean storedSettingsBean,
                                          String dialogTitle) {
    AnalysisScope analysisScope = new AnalysisScope(element.getContainingFile());
    Module module = ModuleUtilCore.findModuleForPsiElement(element);

    Project myProject = element.getProject();
    AnalysisUIOptions analysisUIOptions = new AnalysisUIOptions();
    analysisUIOptions.loadState(storedSettingsBean.analysisUIOptions);

    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(myProject, analysisScope, module, element);
    SliceLanguageSupportProvider provider = LanguageSlicing.getProvider(element);
    boolean supportFilter = provider.supportValueFilters(element);
    class BackwardHandlerDialog extends BaseAnalysisActionDialog {
      JBTextField field;
      
      BackwardHandlerDialog() {
        super(dialogTitle, "Analyze scope", myProject, items, analysisUIOptions, true);
      }

      @Override
      protected @Nullable JComponent getAdditionalActionSettings(Project project) {
        if (!supportFilter) return null;
        JPanel panel = new JPanel(new GridBagLayout());
        JBLabel label = new JBLabel(LangBundle.message("label.filter.value") + " ");
        panel.add(label);
        field = new JBTextField();
        Dimension size = field.getPreferredSize();
        size.width = 400;
        field.setPreferredSize(size);
        panel.add(field);
        label.setLabelFor(field);
        field.getDocument().addDocumentListener(new DocumentAdapter() {
          @Override
          protected void textChanged(@NotNull DocumentEvent e) {
            try {
              getFilter();
              setErrorText(null, field);
            }
            catch (SliceFilterParseException exception) {
              setErrorText(exception.getMessage(), field);
            }
          }
        });
        return panel;
      }

      private @Nullable SliceValueFilter getFilter() throws SliceFilterParseException {
        if (field == null) return null;
        String text = field.getText().trim();
        if (!text.isEmpty()) {
          return provider.parseFilter(element, text);
        }
        return null;
      }
    }
    BackwardHandlerDialog dialog = new BackwardHandlerDialog();
    if (!dialog.showAndGet()) {
      return null;
    }

    AnalysisScope scope = dialog.getScope(analysisScope);
    storedSettingsBean.analysisUIOptions.loadState(analysisUIOptions);

    SliceAnalysisParams params = new SliceAnalysisParams();
    params.scope = scope;
    params.dataFlowToThis = myDataFlowToThis;
    try {
      params.valueFilter = dialog.getFilter();
    }
    catch (SliceFilterParseException ignored) { }
    return params;
  }
}
