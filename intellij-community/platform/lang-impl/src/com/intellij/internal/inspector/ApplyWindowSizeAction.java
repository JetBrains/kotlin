// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.inspector;

import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ApplyWindowSizeAction extends DumbAwareAction {
  public ApplyWindowSizeAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Component owner = IdeFocusManager.findInstance().getFocusOwner();
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    Project project = e.getProject();
    Window window = null;
    if (owner != null) {
      if (editor != null && project != null) {
        LookupEx lookup = LookupManager.getInstance(project).getActiveLookup();
        if (lookup != null) {
          window = ComponentUtil.getParentOfType((Class<? extends Window>)Window.class, lookup.getComponent());
        }
      }

      if (window == null) {
        window = ComponentUtil.getParentOfType((Class<? extends Window>)Window.class, owner);
      }
      if (window != null) {
        int w = ConfigureCustomSizeAction.CustomSizeModel.INSTANCE.getWidth();
        int h = ConfigureCustomSizeAction.CustomSizeModel.INSTANCE.getHeight();
        window.setMinimumSize(new Dimension(w, h));
        window.setSize(w, h);
      }
    }
  }
}
