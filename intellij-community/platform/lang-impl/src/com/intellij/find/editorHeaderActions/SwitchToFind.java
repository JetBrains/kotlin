package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindModel;
import com.intellij.find.FindUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SwitchToFind extends AnAction implements DumbAware {
  public SwitchToFind(@NotNull JComponent shortcutHolder) {
    AnAction findAction = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND);
    if (findAction != null) {
      registerCustomShortcutSet(findAction.getShortcutSet(), shortcutHolder);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    if (search == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    if (KeymapUtil.isEmacsKeymap()) {
      // Emacs users are accustomed to the editor that executes 'find next' on subsequent pressing of shortcut that
      // activates 'incremental search'. Hence, we do the similar hack here for them.
      final EditorAction action = (EditorAction)ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
      action.update(search.getEditor(), e.getPresentation(), e.getDataContext());
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    if (KeymapUtil.isEmacsKeymap()) {
      // Emacs users are accustomed to the editor that executes 'find next' on subsequent pressing of shortcut that
      // activates 'incremental search'. Hence, we do the similar hack here for them.
      final EditorAction action = (EditorAction)ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
      action.actionPerformed(search.getEditor(), e.getDataContext());
      return;
    }

    final FindModel findModel = search.getFindModel();
    FindUtil.configureFindModel(false, e.getData(CommonDataKeys.EDITOR), findModel, false);
    search.getComponent().getSearchTextComponent().selectAll();
  }
}
