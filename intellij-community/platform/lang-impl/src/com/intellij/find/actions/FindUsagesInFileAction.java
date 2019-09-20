// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.find.FindBundle;
import com.intellij.lang.Language;
import com.intellij.lang.findUsages.EmptyFindUsagesProvider;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE;

public class FindUsagesInFileAction extends AnAction implements PossiblyDumbAware {

  public FindUsagesInFileAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean isDumbAware() {
    return Registry.is("ide.find.in.file.highlight.usages");
  }

  @Override
  public boolean startInTransaction() {
    return !Registry.is("ide.find.in.file.highlight.usages");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (Registry.is("ide.find.in.file.highlight.usages")) {
      ActionManager.getInstance().getAction(ACTION_HIGHLIGHT_USAGES_IN_FILE).actionPerformed(e);
      return;
    }
    DataContext dataContext = e.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

    UsageTarget[] usageTargets = UsageView.USAGE_TARGETS_KEY.getData(dataContext);
    if (usageTargets != null) {
      FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(dataContext);
      if (fileEditor != null) {
        usageTargets[0].findUsagesInEditor(fileEditor);
      }
    }
    else if (editor == null) {
      Messages.showMessageDialog(
        project,
        FindBundle.message("find.no.usages.at.cursor.error"),
        CommonBundle.getErrorTitle(),
        Messages.getErrorIcon()
      );
    }
    else {
      HintManager.getInstance().showErrorHint(editor, FindBundle.message("find.no.usages.at.cursor.error"));
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    if (Registry.is("ide.find.in.file.highlight.usages")) {
      ActionManager.getInstance().getAction(ACTION_HIGHLIGHT_USAGES_IN_FILE).update(event);
      return;
    }
    updateFindUsagesAction(event);
  }

  private static boolean isEnabled(DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null ||
        EditorGutter.KEY.getData(dataContext) != null ||
        Boolean.TRUE.equals(dataContext.getData(CommonDataKeys.EDITOR_VIRTUAL_SPACE))) {
      return false;
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      UsageTarget[] target = UsageView.USAGE_TARGETS_KEY.getData(dataContext);
      return target != null && target.length > 0;
    }
    else {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        return false;
      }

      Language language = PsiUtilBase.getLanguageInEditor(editor, project);
      if (language == null) {
        language = file.getLanguage();
      }
      return !(LanguageFindUsages.INSTANCE.forLanguage(language) instanceof EmptyFindUsagesProvider);
    }
  }

  public static void updateFindUsagesAction(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    boolean enabled = isEnabled(dataContext);
    presentation.setVisible(enabled || !ActionPlaces.isPopupPlace(event.getPlace()));
    presentation.setEnabled(enabled);
  }
}
