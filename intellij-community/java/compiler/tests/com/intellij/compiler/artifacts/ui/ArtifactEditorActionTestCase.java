package com.intellij.compiler.artifacts.ui;

import com.intellij.compiler.artifacts.ArtifactsTestUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;

/**
 * @author nik
 */
public abstract class ArtifactEditorActionTestCase extends ArtifactEditorTestCase {

  protected void assertEnabled() {
    assertTrue(isEnabled());
  }

  protected void assertDisabled() {
    assertFalse(isEnabled());
  }

  private boolean isEnabled() {
    final AnAction action = createAction(myArtifactEditor);
    final AnActionEvent event = createActionEvent(action);
    action.update(event);
    return event.getPresentation().isEnabled() && event.getPresentation().isVisible();
  }

  protected void perform() {
    perform(false);
  }

  protected void perform(final boolean confirmationExpected) {
    assertEnabled();
    runAction(() -> {
      final AnAction action = createAction(myArtifactEditor);
      action.actionPerformed(createActionEvent(action));
    }, confirmationExpected);
  }

  protected void assertWillNotBePerformed() {
    final String old = ArtifactsTestUtil.printToString(myArtifactEditor.getRootElement(), 0);
    perform(true);
    assertLayout(old);
  }

  private static AnActionEvent createActionEvent(AnAction action) {
    final Presentation presentation = new Presentation();
    presentation.copyFrom(action.getTemplatePresentation());
    return AnActionEvent.createFromAnAction(action, null, "", DataManager.getInstance().getDataContext());
  }

  protected abstract AnAction createAction(final ArtifactEditorEx artifactEditor);

}
