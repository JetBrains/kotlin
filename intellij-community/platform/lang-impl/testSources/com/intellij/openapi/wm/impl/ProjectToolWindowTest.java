// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewSharedSettings;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowId;

/**
 * @author Dmitry Avdeev
 */
public class ProjectToolWindowTest extends ToolWindowManagerTestCase {
  public void testProjectViewActivate() {
    for (ToolWindowEP extension : ToolWindowEP.EP_NAME.getExtensionList()) {
      if (ToolWindowId.PROJECT_VIEW.equals(extension.id)) {
        myManager.initToolWindow(extension);
      }
    }

    DesktopLayout layout = myManager.getLayout();
    WindowInfoImpl info = layout.getInfo(ToolWindowId.PROJECT_VIEW, false);
    assertFalse(info.isVisible());
    info.setVisible(true);

    ToolWindow window = myManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    assertTrue(window.isVisible());

    ProjectView.getInstance(getProject());

    ProjectViewSharedSettings.Companion.getInstance().setAutoscrollFromSource(true);

    try {
      window.activate(null);
    }
    finally {
      // cleanup
      info.setVisible(false);
    }
  }
}
