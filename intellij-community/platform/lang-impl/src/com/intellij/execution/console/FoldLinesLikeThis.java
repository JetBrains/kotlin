// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class FoldLinesLikeThis extends DumbAwareAction {

  @Nullable
  private static String getSingleLineSelection(@NotNull Editor editor) {
    final SelectionModel model = editor.getSelectionModel();
    final Document document = editor.getDocument();
    if (!model.hasSelection()) {
      final int offset = editor.getCaretModel().getOffset();
      if (offset <= document.getTextLength()) {
        final int lineNumber = document.getLineNumber(offset);
        final String line = document.getText().substring(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)).trim();
        if (StringUtil.isNotEmpty(line)) {
          return line;
        }
      }

      return null;
    }
    final int start = model.getSelectionStart();
    final int end = model.getSelectionEnd();
    if (document.getLineNumber(start) == document.getLineNumber(end)) {
      final String selection = document.getText().substring(start, end).trim();
      if (StringUtil.isNotEmpty(selection)) {
        return selection;
      }
    }
    return null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);

    final boolean enabled = e.getData(LangDataKeys.CONSOLE_VIEW) != null &&  editor != null && getSingleLineSelection(editor) != null;
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    assert editor != null;
    final String selection = getSingleLineSelection(editor);
    assert selection != null;
    ShowSettingsUtil.getInstance().editConfigurable(editor.getProject(), new ConsoleConfigurable() {
      @Override
      protected boolean editFoldingsOnly() {
        return true;
      }

      @Override
      public void reset() {
        super.reset();
        UIUtil.invokeLaterIfNeeded(() -> addRule(selection));
      }
    });
    final ConsoleView consoleView = e.getData(LangDataKeys.CONSOLE_VIEW);
    if (consoleView instanceof ConsoleViewImpl) {
      ((ConsoleViewImpl)consoleView).foldImmediately();
    }
  }
}
