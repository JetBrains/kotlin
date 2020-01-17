// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewState;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dmitry Avdeev
 */
public class ProjectToolWindowTest extends ToolWindowManagerTestCase {
  public void testProjectViewActivate() {
    for (ToolWindowEP extension : ToolWindowEP.EP_NAME.getExtensionList()) {
      if (ToolWindowId.PROJECT_VIEW.equals(extension.id)) {
        manager.initToolWindow(extension);
      }
    }

    DesktopLayout layout = manager.getLayout();
    WindowInfoImpl info = layout.getInfo(ToolWindowId.PROJECT_VIEW);
    assertThat(info.isVisible()).isFalse();
    info.setVisible(true);

    ToolWindow window = manager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    // because change is not applied from desktop
    assertThat(window.isVisible()).isFalse();

    manager.showToolWindow(ToolWindowId.PROJECT_VIEW);
    assertThat(window.isVisible()).isTrue();

    ProjectView.getInstance(getProject());

    ProjectViewState.getInstance(getProject()).setAutoscrollFromSource(true);

    try {
      window.activate(null);
    }
    finally {
      // cleanup
      info.setVisible(false);
    }
  }
}
