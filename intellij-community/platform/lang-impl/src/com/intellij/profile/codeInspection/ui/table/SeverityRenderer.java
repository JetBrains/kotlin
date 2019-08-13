// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui.table;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.SeverityEditorDialog;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.profile.codeInspection.ui.LevelChooserAction;
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.stream.Stream;

public class SeverityRenderer extends ComboBoxTableRenderer<HighlightSeverity> {
  static final HighlightSeverity EDIT_SEVERITIES = new HighlightSeverity(InspectionsBundle.message("inspection.edit.severities.text"), -1);
  @NotNull
  private final Runnable myOnClose;
  private final ScopesAndSeveritiesTable myTable;
  @NotNull
  private final Icon myDisabledIcon;
  @NotNull
  private final Project myProject;

  public SeverityRenderer(@NotNull InspectionProfileImpl inspectionProfile,
                          @NotNull Project project,
                          @NotNull Runnable onClose,
                          @NotNull ScopesAndSeveritiesTable table) {
    super(getSeverities(inspectionProfile));
    myOnClose = onClose;
    myTable = table;
    myDisabledIcon = HighlightDisplayLevel.createIconByMask(UIUtil.getLabelDisabledForeground());
    myProject = project;
  }

  @NotNull
  public static HighlightSeverity[] getSeverities(InspectionProfileImpl inspectionProfile) {
    Stream<HighlightSeverity>
      severities = LevelChooserAction.getSeverities(inspectionProfile.getProfileManager().getSeverityRegistrar()).stream();
    return StreamEx.of(severities).append(EDIT_SEVERITIES).toArray(HighlightSeverity.class);
  }

  public static Icon getIcon(@NotNull HighlightDisplayLevel level) {
    Icon icon = level.getIcon();
    return icon instanceof HighlightDisplayLevel.ColoredIcon
                 ? new ColorIcon(icon.getIconWidth(), ((HighlightDisplayLevel.ColoredIcon)icon).getColor())
                 : icon;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    component.setEnabled(((ScopesAndSeveritiesTable)table).isRowEnabled(row));
    return component;
  }

  @Override
  protected void customizeComponent(HighlightSeverity value, JTable table, boolean isSelected) {
    super.customizeComponent(value, table, isSelected);
    setDisabledIcon(myDisabledIcon);
  }

  @Override
  protected String getTextFor(@NotNull final HighlightSeverity value) {
    return SingleInspectionProfilePanel.renderSeverity(value);
  }

  @Override
  protected Icon getIconFor(@NotNull final HighlightSeverity value) {
    return value == EDIT_SEVERITIES
           ? EmptyIcon.create(HighlightDisplayLevel.getEmptyIconDim())
           : getIcon(HighlightDisplayLevel.find(value));
  }

  @Override
  public boolean isCellEditable(final EventObject event) {
    return !(event instanceof MouseEvent) || ((MouseEvent)event).getClickCount() >= 1;
  }

  @Override
  protected ListSeparator getSeparatorAbove(HighlightSeverity value) {
    return value == EDIT_SEVERITIES ? new ListSeparator() : null;
  }

  @Override
  public void onClosed(@NotNull LightweightWindowEvent event) {
    super.onClosed(event);
    myOnClose.run();
    if (getCellEditorValue() == EDIT_SEVERITIES) {
      ApplicationManager.getApplication().invokeLater(() ->
                                                        SeverityEditorDialog.show(myProject, null, SeverityRegistrar.getSeverityRegistrar(myProject), true, severity ->
                                                          myTable.setSelectedSeverity(severity)));
    }
  }
}
