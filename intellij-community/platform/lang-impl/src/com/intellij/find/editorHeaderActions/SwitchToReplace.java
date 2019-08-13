package com.intellij.find.editorHeaderActions;

import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindModel;
import com.intellij.find.FindUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SwitchToReplace extends AnAction {
  public SwitchToReplace(@NotNull JComponent shortcutHolder) {
    AnAction replaceAction = ActionManager.getInstance().getAction("Replace");
    if (replaceAction != null) {
      registerCustomShortcutSet(replaceAction.getShortcutSet(), shortcutHolder);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Editor editor = e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabled(editor != null && search != null && !ConsoleViewUtil.isConsoleViewEditor(editor));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    FindModel findModel = search.getFindModel();
    FindUtil.configureFindModel(true, e.getData(CommonDataKeys.EDITOR), findModel, false);
    search.getComponent().getSearchTextComponent().selectAll();
  }
}
