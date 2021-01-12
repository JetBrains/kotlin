package com.intellij.find.editorHeaderActions;

import com.intellij.find.FindBundle;
import com.intellij.find.FindSettings;
import com.intellij.find.SearchSession;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ToggleWholeWordsOnlyAction extends EditorHeaderToggleAction implements Embeddable {
  public ToggleWholeWordsOnlyAction() {
    super(FindBundle.message("find.whole.words"),
          AllIcons.Actions.Words,
          AllIcons.Actions.WordsHovered,
          AllIcons.Actions.WordsSelected);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    SearchSession session = e.getData(SearchSession.KEY);
    e.getPresentation().setEnabled(session != null && !session.getFindModel().isRegularExpressions());
    e.getPresentation().setVisible(session != null && !session.getFindModel().isMultiline());

    super.update(e);
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
