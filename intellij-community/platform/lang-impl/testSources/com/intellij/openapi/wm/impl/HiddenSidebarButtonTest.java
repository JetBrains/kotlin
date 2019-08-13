// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.usageView.impl.UsageViewContentManagerImpl;

import java.util.Arrays;

/**
 * @author Vassiliy Kudryashov
 */
public class HiddenSidebarButtonTest extends ToolWindowManagerTestCase {
  private static final String LAYOUT = "<layout>" +
                                 "<window_info id=\"TODO\" active=\"false\" anchor=\"bottom\" auto_hide=\"false\" internal_type=\"DOCKED\" type=\"DOCKED\" visible=\"false\" show_stripe_button=\"false\" weight=\"0.42947903\" sideWeight=\"0.4874552\" order=\"6\" side_tool=\"false\" content_ui=\"tabs\" x=\"119\" y=\"106\" width=\"619\" height=\"748\"/>" +
                                 "<window_info id=\"Find\" active=\"false\" anchor=\"bottom\" auto_hide=\"false\" internal_type=\"DOCKED\" type=\"DOCKED\" visible=\"false\" show_stripe_button=\"true\" weight=\"0.47013977\" sideWeight=\"0.5\" order=\"1\" side_tool=\"false\" content_ui=\"tabs\" x=\"443\" y=\"301\" width=\"702\" height=\"388\"/>" +
                                 "<window_info id=\"Project\" active=\"false\" anchor=\"left\" auto_hide=\"false\" internal_type=\"DOCKED\" type=\"DOCKED\" visible=\"false\" show_stripe_button=\"true\" weight=\"0.37235227\" sideWeight=\"0.6060991\" order=\"0\" side_tool=\"false\" content_ui=\"tabs\" x=\"116\" y=\"80\" width=\"487\" height=\"787\"/>" +
                                 "</layout>";

  private static final String[] IDS = {ToolWindowId.TODO_VIEW, ToolWindowId.FIND, ToolWindowId.PROJECT_VIEW};
  private static final boolean[] ESTIMATED_TO_SHOW = {false, true, true};
  private static final boolean[] ESTIMATED_VISIBILITY = {false, false, true};

  public void testHiddenButton() throws Exception {
    DesktopLayout layout = myManager.getLayout();
    layout.readExternal(JDOMUtil.load(LAYOUT));
    for (String ID : IDS) {
      assertFalse(layout.isToolWindowRegistered(ID));
    }

    for (ToolWindowEP extension : ToolWindowEP.EP_NAME.getExtensionList()) {
      if (Arrays.asList(ToolWindowId.TODO_VIEW, ToolWindowId.FIND, ToolWindowId.PROJECT_VIEW).contains(extension.id)) {
        myManager.initToolWindow(extension);
      }
    }
    new UsageViewContentManagerImpl(myManager.getProject(), myManager);

    for (int i = 0; i < IDS.length; i++) {
      assertTrue(layout.isToolWindowRegistered(IDS[i]));
      assertEquals(ESTIMATED_TO_SHOW[i], layout.getInfo(IDS[i], true).isShowStripeButton());
      assertEquals(ESTIMATED_VISIBILITY[i], myManager.getStripeButton(IDS[i]).isVisible());
    }
  }
}
