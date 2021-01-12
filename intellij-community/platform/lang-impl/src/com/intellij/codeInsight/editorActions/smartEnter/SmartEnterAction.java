// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.smartEnter;

import com.intellij.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartEnterAction extends EditorAction {
  public SmartEnterAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  private static class Handler extends EditorWriteActionHandler {
    Handler() {
      super(true);
    }

    @Override
    public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
      return getEnterHandler().isEnabled(editor, caret, dataContext);
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
      Project project = CommonDataKeys.PROJECT.getData(dataContext);
      if (project == null || editor.isOneLineMode()) {
        plainEnter(editor, caret, dataContext);
        return;
      }

      LookupManager.getInstance(project).hideActiveLookup();

      TemplateState state = TemplateManagerImpl.getTemplateState(editor);
      if (state != null) {
        state.gotoEnd();
      }

      final int caretOffset = editor.getCaretModel().getOffset();

      PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
      if (psiFile == null) {
        plainEnter(editor, caret, dataContext);
        return;
      }

      if (EnterAfterUnmatchedBraceHandler.isAfterUnmatchedLBrace(editor, caretOffset, psiFile.getFileType())) {
        EditorActionHandler enterHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        enterHandler.execute(editor, caret, dataContext);
        return;
      }

      final Language language = PsiUtilBase.getLanguageInEditor(editor, project);
      boolean processed = false;
      if (language != null) {
        final List<SmartEnterProcessor> processors = SmartEnterProcessors.INSTANCE.allForLanguage(language);
        if (!processors.isEmpty()) {
          for (SmartEnterProcessor processor : processors) {
            if (processor.process(project, editor, psiFile)) {
              processed = true;
              break;
            }
          }
        }
      }
      if (!processed) {
        plainEnter(editor, caret, dataContext);
      }
    }
  }

  public static void plainEnter(Editor editor, Caret caret, DataContext dataContext) {
    getEnterHandler().execute(editor, caret, dataContext);
  }

  private static EditorActionHandler getEnterHandler() {
    return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE);
  }
}

