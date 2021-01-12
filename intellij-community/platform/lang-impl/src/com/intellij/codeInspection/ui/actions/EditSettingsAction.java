// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui.actions;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.EditInspectionToolsSettingsAction;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * @author Dmitry Batkovich
 */
public class EditSettingsAction extends InspectionViewActionBase {
  private static final Logger LOG = Logger.getInstance(EditSettingsAction.class);

  public EditSettingsAction() {
    super(InspectionsBundle.messagePointer("inspection.action.edit.settings"), Presentation.NULL_STRING, AllIcons.General.Settings);
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
    return Objects.requireNonNull(profile.getInspectionTool(Objects.requireNonNull(profile.getSingleTool()), view.getProject()));
  }
}
