package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ToggleSelectionOnlyAction extends EditorHeaderToggleAction {
  public ToggleSelectionOnlyAction() {
    super("In &Selection");
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    EditorSearchSession session = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabledAndVisible(session != null && session.getFindModel().isReplaceState());
  }

  @Override
  protected boolean isSelected(@NotNull SearchSession session) {
    return !session.getFindModel().isGlobal();
  }

  @Override
  protected void setSelected(@NotNull SearchSession session, boolean selected) {
    session.getFindModel().setGlobal(!selected);
  }
}
