// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.actions.RunInspectionAction;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeTable;
import com.intellij.ui.ClickListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBLabelDecorator;
import com.intellij.ui.components.panels.StatelessCardLayout;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Batkovich
 */
public class InspectionNodeInfo extends JPanel {
  private final static Logger LOG = Logger.getInstance(InspectionNodeInfo.class);

  public InspectionNodeInfo(@NotNull final InspectionTree tree,
                            @NotNull final Project project) {
    setLayout(new GridBagLayout());
    setBorder(JBUI.Borders.emptyTop(11));
    final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper(false);
    LOG.assertTrue(toolWrapper != null);
    InspectionProfileImpl currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
    boolean enabled = currentProfile.getTools(toolWrapper.getShortName(), project).isEnabled();

    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
    JBLabelDecorator label = JBLabelDecorator.createJBLabelDecorator().setBold(true);
    label.setText(toolWrapper.getDisplayName() + " inspection");
    titlePanel.add(label);
    titlePanel.add(Box.createHorizontalStrut(JBUIScale.scale(16)));
    if (!enabled) {
      JBLabel enabledLabel = new JBLabel();
      enabledLabel.setForeground(JBColor.GRAY);
      enabledLabel.setText("Disabled");
      titlePanel.add(enabledLabel);
    }

    add(titlePanel,
        new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new JBInsets(0, 12, 5, 16),
                               0, 0));

    JEditorPane description = new JEditorPane();
    description.setContentType(UIUtil.HTML_MIME);
    description.setEditable(false);
    description.setOpaque(false);
    description.setBackground(UIUtil.getLabelBackground());
    description.addHyperlinkListener(SingleInspectionProfilePanel.createSettingsHyperlinkListener(project));
    String descriptionText = toolWrapper.loadDescription();
    if (descriptionText == null) {
      InspectionEP extension = toolWrapper.getExtension();
      LOG.error(new PluginException("Inspection #" + toolWrapper.getShortName() + " has no description", extension != null ? extension.getPluginId() : null));
    }
    final String toolDescription =
      stripUIRefsFromInspectionDescription(StringUtil.notNullize(descriptionText));
    SingleInspectionProfilePanel.readHTML(description, SingleInspectionProfilePanel.toHTML(description, toolDescription == null ? "" : toolDescription, false));
    JScrollPane pane = ScrollPaneFactory.createScrollPane(description, true);
    int maxWidth = getFontMetrics(UIUtil.getLabelFont()).charWidth('f') * 110 - pane.getMinimumSize().width;
    pane.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
    pane.setAlignmentX(0);

    add(StatelessCardLayout.wrap(pane),
        new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                               new JBInsets(0, 10, 0, 0), 0, 0));

    JButton enableButton = null;
    if (currentProfile.getSingleTool() != null) {
      enableButton = new JButton((enabled ? "Disable" : "Enable") + " inspection");
      new ClickListener() {
        @Override
        public boolean onClick(@NotNull MouseEvent event, int clickCount) {
          InspectionsConfigTreeTable.setToolEnabled(!enabled, currentProfile, toolWrapper.getShortName(), project);
          tree.getContext().getView().profileChanged();
          return true;
        }
      }.installOn(enableButton);
    }

    JButton runInspectionOnButton = new JButton(InspectionsBundle.message("run.inspection.on.file.intention.text"));
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent event, int clickCount) {
        RunInspectionAction.runInspection(project, toolWrapper.getShortName(), null, null, null);
        return true;
      }
    }.installOn(runInspectionOnButton);

    JPanel buttons = new JPanel();
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
    if (enableButton != null) {
      buttons.add(enableButton);
    }
    buttons.add(Box.createHorizontalStrut(JBUIScale.scale(3)));
    buttons.add(runInspectionOnButton);

    add(buttons,
        new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                               new JBInsets(15, 9, 9, 0), 0, 0));

  }

  public static String stripUIRefsFromInspectionDescription(@NotNull String description) {
    final int descriptionEnd = description.indexOf("<!-- tooltip end -->");
    if (descriptionEnd < 0) {
      final Pattern pattern = Pattern.compile(".*Use.*(the (panel|checkbox|checkboxes|field|button|controls).*below).*", Pattern.DOTALL);
      final Matcher matcher = pattern.matcher(description);
      int startFindIdx = 0;
      while (matcher.find(startFindIdx)) {
        final int end = matcher.end(1);
        startFindIdx = end;
        description = description.substring(0, matcher.start(1)) + " inspection settings " + description.substring(end);
      }
    } else {
      description = description.substring(0, descriptionEnd);
    }
    return description;
  }
}
