/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.actions.BaseViewAction;
import com.intellij.execution.ui.layout.Tab;
import com.intellij.execution.ui.layout.ViewContext;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.content.Content;

public class MinimizeViewAction extends BaseViewAction {

  @Override
  protected void update(final AnActionEvent e, final ViewContext context, final Content[] content) {
    setEnabled(e, isEnabled(context, content, e.getPlace()));
    e.getPresentation().setIcon(AllIcons.Actions.Move_to_button);
  }

  @Override
  protected void actionPerformed(final AnActionEvent e, final ViewContext context, final Content[] content) {
    for (Content each : content) {
      context.findCellFor(each).minimize(each);
    }
  }

  public static boolean isEnabled(ViewContext context, Content[] content, String place) {
    if (!context.isMinimizeActionEnabled() || content.length == 0) {
      return false;
    }

    if (ViewContext.TAB_TOOLBAR_PLACE.equals(place) || ViewContext.TAB_POPUP_PLACE.equals(place)) {
      Tab tab = getTabFor(context, content);
      if (tab == null) {
        return false;
      }
      return !tab.isDefault() && content.length == 1;
    }
    else {
      return getTabFor(context, content) != null;
    }
  }
}
