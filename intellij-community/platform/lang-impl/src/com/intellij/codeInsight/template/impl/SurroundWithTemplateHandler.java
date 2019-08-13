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

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author mike
 */
public class SurroundWithTemplateHandler implements CodeInsightActionHandler {
  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    if (!EditorModificationUtil.checkModificationAllowed(editor)) return;
    if (!editor.getSelectionModel().hasSelection()) {
      SurroundWithHandler.selectLogicalLineContentsAtCaret(editor);
      if (!editor.getSelectionModel().hasSelection()) return;
    }

    List<AnAction> group = createActionGroup(editor, file, new HashSet<>());
    if (group.isEmpty()) {
      HintManager.getInstance().showErrorHint(editor, CodeInsightBundle.message("templates.surround.no.defined"));
      return;
    }


    ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(CodeInsightBundle.message("templates.select.template.chooser.title"), new DefaultActionGroup(group),
                              DataManager.getInstance().getDataContext(editor.getContentComponent()),
                              JBPopupFactory.ActionSelectionAid.MNEMONICS, false);

    popup.showInBestPositionFor(editor);
  }

  @NotNull
  public static List<AnAction> createActionGroup(@NotNull Editor editor, @NotNull PsiFile file, @NotNull Set<Character> usedMnemonicsSet) {
    List<CustomLiveTemplate> customTemplates = TemplateManagerImpl.listApplicableCustomTemplates(editor, file, true);
    List<TemplateImpl> templates = TemplateManagerImpl.listApplicableTemplateWithInsertingDummyIdentifier(editor, file, true);
    if (templates.isEmpty() && customTemplates.isEmpty()) {
      return Collections.emptyList();
    }

    List<AnAction> group = new ArrayList<>();

    for (TemplateImpl template : templates) {
      group.add(new InvokeTemplateAction(template, editor, file.getProject(), usedMnemonicsSet,
                                 () -> SurroundWithLogger.logTemplate(template, file.getLanguage(), file.getProject())));
    }

    for (CustomLiveTemplate customTemplate : customTemplates) {
      group.add(new WrapWithCustomTemplateAction(customTemplate, editor, file, usedMnemonicsSet, () -> SurroundWithLogger.
        logCustomTemplate(customTemplate, file.getLanguage(), file.getProject())));
    }
    return group;
  }
}
