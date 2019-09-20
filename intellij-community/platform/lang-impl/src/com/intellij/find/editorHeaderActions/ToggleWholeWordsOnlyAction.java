package com.intellij.find.editorHeaderActions;

import com.intellij.find.FindSettings;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ToggleWholeWordsOnlyAction extends EditorHeaderToggleAction {
  public ToggleWholeWordsOnlyAction() {
    super("W&ords");
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    SearchSession session = e.getData(SearchSession.KEY);
    e.getPresentation().setEnabled(session != null && !session.getFindModel().isRegularExpressions());
    e.getPresentation().setVisible(session != null && !session.getFindModel().isMultiline());
  }

  @Override
  protected boolean isSelected(@NotNull SearchSession session) {
    return session.getFindModel().isWholeWordsOnly();
  }

  @Override
  protected void setSelected(@NotNull SearchSession session, boolean selected) {
    FindSettings.getInstance().setLocalWholeWordsOnly(selected);
    session.getFindModel().setWholeWordsOnly(selected);
  }
}
