/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.ide.navigationToolbar;

import com.intellij.ide.ui.UISettings;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ShowNavBarAction extends AnAction implements DumbAware, PopupAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e){
    final DataContext context = e.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project != null) {
      UISettings uiSettings = UISettings.getInstance();
      if (uiSettings.getShowNavigationBar() && !uiSettings.getPresentationMode()){
        new SelectInNavBarTarget(project).select(null, false);
      } else {
        final Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(context);
        if (!isInsideNavBar(component)) {
          final Editor editor = CommonDataKeys.EDITOR.getData(context);
          final NavBarPanel toolbarPanel = new NavBarPanel(project, false);
          toolbarPanel.showHint(editor, context);
        }
      }
    }
  }

  private static boolean isInsideNavBar(Component c) {
    return c == null
           || c instanceof NavBarPanel
           || UIUtil.getParentOfType(NavBarListWrapper.class, c) != null;
  }


  @Override
  public void update(@NotNull final AnActionEvent e){
    final boolean enabled = e.getData(CommonDataKeys.PROJECT) != null;
    e.getPresentation().setEnabled(enabled);

    // see RIDER-15982
    if (!ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
      e.getPresentation().setText(ActionsBundle.message("action.ShowNavBar.ShortText"));
    }
  }
}
