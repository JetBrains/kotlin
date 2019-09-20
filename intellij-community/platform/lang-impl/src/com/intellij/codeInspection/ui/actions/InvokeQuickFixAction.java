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

package com.intellij.codeInspection.ui.actions;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.InspectionRVContentProvider;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

public class InvokeQuickFixAction extends AnAction {
  private final InspectionResultsView myView;

  public InvokeQuickFixAction(final InspectionResultsView view) {
    super(InspectionsBundle.message("inspection.action.apply.quickfix"), InspectionsBundle.message("inspection.action.apply.quickfix.description"), AllIcons.Actions.IntentionBulb);
    myView = view;
    registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS).getShortcutSet(),
                              myView.getTree());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper(true);
    final InspectionRVContentProvider provider = myView.getProvider();
    if (toolWrapper == null || cantApplyFixes(myView)) {
      presentation.setEnabled(false);
      return;
    }
    presentation.setEnabled(provider.hasQuickFixes(myView.getTree()));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final ActionGroup fixes = (ActionGroup)ActionManager.getInstance().getAction("QuickFixes");
    if (fixes.getChildren(e).length == 0) {
      Messages.showInfoMessage(myView, "There are no applicable quick fixes", "Nothing Found to Fix");
      return;
    }
    DataContext dataContext = e.getDataContext();
    final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(InspectionsBundle.message("inspection.tree.popup.title"),
                              fixes,
                              dataContext,
                              JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                              false);
    InspectionResultsView.showPopup(e, popup);
  }

  static boolean cantApplyFixes(InspectionResultsView view) {
    return !view.getTree().areDescriptorNodesSelected();
  }
}
