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
package com.intellij.codeInspection.ui.actions;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.EditInspectionToolsSettingsAction;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Dmitry Batkovich
 */
public class EditSettingsAction extends InspectionViewActionBase {
  private static final Logger LOG = Logger.getInstance(EditSettingsAction.class);

  public EditSettingsAction() {
    super(InspectionsBundle.message("inspection.action.edit.settings"),
          null,
          AllIcons.General.Settings);
  }

  @Override
  protected boolean isEnabled(@NotNull InspectionResultsView view, AnActionEvent e) {
    boolean enabled = view.areSettingsEnabled();
    e.getPresentation().setDescription(enabled
                                       ? InspectionsBundle.message("inspection.action.edit.settings")
                                       : InspectionsBundle.message("inspection.tool.window.dialog.no.options", getSingleTool(view).getDisplayName()));
    return enabled;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final InspectionResultsView view = getView(e);
    InspectionProfileImpl inspectionProfile = view.getCurrentProfile();

    if (view.isSingleInspectionRun()) {
      InspectionToolWrapper tool = getSingleTool(view);
      JComponent panel = tool.getTool().createOptionsPanel();
      LOG.assertTrue(panel != null, "Unexpectedly inspection '" + tool.getShortName() + "' didn't create an options panel");
      final DialogBuilder builder = new DialogBuilder()
        .title(InspectionsBundle.message("inspection.tool.window.inspection.dialog.title", tool.getDisplayName()))
        .centerPanel(panel);
      builder.removeAllActions();
      builder.addOkAction();
      if (view.isRerunAvailable()) {
        builder.addActionDescriptor(new DialogBuilder.DialogActionDescriptor(InspectionsBundle.message("inspection.action.rerun"), 'R') {
          @Override
          protected Action createAction(DialogWrapper dialogWrapper) {
            return new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                view.rerun();
                dialogWrapper.close(DialogWrapper.OK_EXIT_CODE);
              }
            };
          }
        });
      }
      builder.show();
    } else {
      final InspectionToolWrapper toolWrapper = view.getTree().getSelectedToolWrapper(false);
      if (toolWrapper != null) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName()); //do not search for dead code entry point tool
        if (key != null) {
          new EditInspectionToolsSettingsAction(key).editToolSettings(view.getProject(), inspectionProfile);
          return;
        }
      }

      final String[] path = view.getTree().getSelectedGroupPath();
      EditInspectionToolsSettingsAction.editSettings(view.getProject(), inspectionProfile, (c) -> {
        if (path != null) {
          c.selectInspectionGroup(path);
        }
      });
    }
  }

  @NotNull
  private static InspectionToolWrapper getSingleTool(InspectionResultsView view) {
    InspectionProfileImpl profile = view.getCurrentProfile();
    return ObjectUtils.notNull(profile.getInspectionTool(ObjectUtils.notNull(profile.getSingleTool()), view.getProject()));
  }
}
