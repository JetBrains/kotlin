// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.impl.CachedIntentions;
import com.intellij.codeInsight.intention.impl.IntentionHintComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class IntentionsUIImpl extends IntentionsUI {

  private volatile IntentionHintComponent myLastIntentionHint;

  public IntentionsUIImpl(Project project) {
    super(project);
  }

  IntentionHintComponent getLastIntentionHint() {
    return myLastIntentionHint;
  }

  @Override
  public void update(@NotNull CachedIntentions cachedIntentions, boolean actionsChanged) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Editor editor = cachedIntentions.getEditor();
    if (editor == null) return;
    if (!ApplicationManager.getApplication().isUnitTestMode() && !editor.getContentComponent().hasFocus()) return;
    if (!actionsChanged) return;

    //IntentionHintComponent hint = myLastIntentionHint;
    //if (hint != null && hint.getPopupUpdateResult(actionsChanged) == IntentionHintComponent.PopupUpdateResult.CHANGED_INVISIBLE) {
    //  hint.recreate();
    //  return;
    //}

    Project project = cachedIntentions.getProject();
    LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    Point xy = editor.logicalPositionToXY(caretPos);

    hide();
    if (!HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(false) &&
        visibleArea.contains(xy) &&
        editor.getSettings().isShowIntentionBulb() &&
        editor.getCaretModel().getCaretCount() == 1 &&
        cachedIntentions.showBulb()) {
      myLastIntentionHint = IntentionHintComponent.showIntentionHint(project, cachedIntentions.getFile(), editor, false, cachedIntentions);
    }
  }

  @Override
  public void hide() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    IntentionHintComponent hint = myLastIntentionHint;
    if (hint != null && hint.isVisible()) {
      hint.hide();
      myLastIntentionHint = null;
    }
  }
}
