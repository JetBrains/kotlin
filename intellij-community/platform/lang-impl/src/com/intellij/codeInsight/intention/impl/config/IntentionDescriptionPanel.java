// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.*;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HintHint;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

// used in Rider
public class IntentionDescriptionPanel {
  private static final Logger LOG = Logger.getInstance(IntentionDescriptionPanel.class);
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;
  private JPanel myPoweredByPanel;
  private JPanel myDescriptionPanel;
  private final List<IntentionUsagePanel> myBeforeUsagePanels = new ArrayList<>();
  private final List<IntentionUsagePanel> myAfterUsagePanels = new ArrayList<>();
  @NonNls private static final String BEFORE_TEMPLATE = "before.java.template";
  @NonNls private static final String AFTER_TEMPLATE = "after.java.template";

  public IntentionDescriptionPanel() {
    myBeforePanel.setBorder(IdeBorderFactory.createTitledBorder("Before:", false, JBUI.insetsTop(8)).setShowLine(false));
    myAfterPanel.setBorder(IdeBorderFactory.createTitledBorder("After:", false, JBUI.insetsTop(8)).setShowLine(false));
    myPoweredByPanel.setBorder(IdeBorderFactory.createTitledBorder(
      CodeInsightBundle.message("powered.by"), false, JBUI.insetsTop(8)).setShowLine(false));

    myDescriptionBrowser.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        BrowserUtil.browse(e.getURL());
      }
    });
  }

  // TODO 134099: see SingleInspectionProfilePanel#readHTML and and PostfixDescriptionPanel#readHtml
  private boolean readHTML(String text) {
    try {
      myDescriptionBrowser.read(new StringReader(text), null);
      return true;
    }
    catch (IOException ignored) {
      return false;
    }
  }

  // TODO 134099: see SingleInspectionProfilePanel#setHTML and PostfixDescriptionPanel#readHtml
  private String toHTML(String text) {
    final HintHint hintHint = new HintHint(myDescriptionBrowser, new Point(0, 0));
    hintHint.setFont(StartupUiUtil.getLabelFont());
    return HintUtil.prepareHintText(text, hintHint);
  }

  public void reset(IntentionActionMetaData actionMetaData, String filter)  {
    try {
      final TextDescriptor url = actionMetaData.getDescription();
      final String description = StringUtil.isEmpty(url.getText()) ?
                                 toHTML(CodeInsightBundle.message("under.construction.string")) :
                                 SearchUtil.markup(toHTML(url.getText()), filter);
      readHTML(description);
      setupPoweredByPanel(actionMetaData);

      showUsages(myBeforePanel, myBeforeUsagePanels, actionMetaData.getExampleUsagesBefore());
      showUsages(myAfterPanel, myAfterUsagePanels, actionMetaData.getExampleUsagesAfter());

      SwingUtilities.invokeLater(() -> myPanel.revalidate());

    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private void setupPoweredByPanel(final IntentionActionMetaData actionMetaData) {
    PluginId pluginId = actionMetaData == null ? null : actionMetaData.getPluginId();
    myPoweredByPanel.removeAll();
    IdeaPluginDescriptorImpl pluginDescriptor  = (IdeaPluginDescriptorImpl)PluginManagerCore.getPlugin(pluginId);
    boolean isCustomPlugin = pluginDescriptor != null && pluginDescriptor.isBundled();
    if (isCustomPlugin) {
      HyperlinkLabel label = new HyperlinkLabel(CodeInsightBundle.message("powered.by.plugin", pluginDescriptor.getName()));
      label.addHyperlinkListener(__ -> {
        Project project = ProjectManager.getInstance().getDefaultProject();
        PluginManagerConfigurableProxy.showPluginConfigurable(null, project, pluginDescriptor);
      });
      myPoweredByPanel.add(label, BorderLayout.CENTER);
    }
    myPoweredByPanel.setVisible(isCustomPlugin);
  }


  public void reset(String intentionCategory)  {
    try {
      readHTML(toHTML(CodeInsightBundle.message("intention.settings.category.text", intentionCategory)));
      setupPoweredByPanel(null);

      String pathForPackage = getClass().getPackage().getName().replace('.', '/');
      TextDescriptor beforeTemplate = new ResourceTextDescriptor(getClass().getClassLoader(), pathForPackage + "/" + BEFORE_TEMPLATE);
      showUsages(myBeforePanel, myBeforeUsagePanels, new TextDescriptor[]{beforeTemplate});
      TextDescriptor afterTemplate = new ResourceTextDescriptor(getClass().getClassLoader(), pathForPackage + "/" + AFTER_TEMPLATE);
      showUsages(myAfterPanel, myAfterUsagePanels, new TextDescriptor[]{afterTemplate});

      SwingUtilities.invokeLater(() -> myPanel.revalidate());
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void showUsages(final JPanel panel,
                                 final List<IntentionUsagePanel> usagePanels,
                                 @Nullable final TextDescriptor[] exampleUsages) throws IOException {
    GridBagConstraints gb = null;
    boolean reuse = exampleUsages != null && panel.getComponents().length == exampleUsages.length;
    if (!reuse) {
      disposeUsagePanels(usagePanels);
      panel.setLayout(new GridBagLayout());
      panel.removeAll();
      gb = new GridBagConstraints();
      gb.anchor = GridBagConstraints.NORTHWEST;
      gb.fill = GridBagConstraints.BOTH;
      gb.gridheight = GridBagConstraints.REMAINDER;
      gb.gridwidth = 1;
      gb.gridx = 0;
      gb.gridy = 0;
      gb.insets = new Insets(0,0,0,0);
      gb.ipadx = 5;
      gb.ipady = 5;
      gb.weightx = 1;
      gb.weighty = 1;
    }

    if (exampleUsages != null) {
      for (int i = 0; i < exampleUsages.length; i++) {
        final TextDescriptor exampleUsage = exampleUsages[i];
        final String name = exampleUsage.getFileName();
        final FileTypeManagerEx fileTypeManager = FileTypeManagerEx.getInstanceEx();
        final String extension = fileTypeManager.getExtension(name);
        final FileType fileType = fileTypeManager.getFileTypeByExtension(extension);

        IntentionUsagePanel usagePanel;
        if (reuse) {
          usagePanel = (IntentionUsagePanel)panel.getComponent(i);
        }
        else {
          usagePanel = new IntentionUsagePanel();
          usagePanels.add(usagePanel);
        }
        usagePanel.reset(exampleUsage.getText(), fileType);

        if (!reuse) {
          panel.add(usagePanel, gb);
          gb.gridx++;
        }
      }
    }
    panel.revalidate();
    panel.repaint();
  }

  public JPanel getComponent() {
    return myPanel;
  }

  public void dispose() {
    disposeUsagePanels(myBeforeUsagePanels);
    disposeUsagePanels(myAfterUsagePanels);
  }

  private static void disposeUsagePanels(List<? extends IntentionUsagePanel> usagePanels) {
    for (final IntentionUsagePanel usagePanel : usagePanels) {
      Disposer.dispose(usagePanel);
    }
    usagePanels.clear();
  }

  public void init(final int preferredWidth) {
    //adjust vertical dimension to be equal for all three panels
    double height = (myDescriptionPanel.getSize().getHeight() + myBeforePanel.getSize().getHeight() + myAfterPanel.getSize().getHeight()) / 3;
    final Dimension newd = new Dimension(preferredWidth, (int)height);
    myDescriptionPanel.setSize(newd);
    myDescriptionPanel.setPreferredSize(newd);
    myDescriptionPanel.setMaximumSize(newd);
    myDescriptionPanel.setMinimumSize(newd);

    myBeforePanel.setSize(newd);
    myBeforePanel.setPreferredSize(newd);
    myBeforePanel.setMaximumSize(newd);
    myBeforePanel.setMinimumSize(newd);

    myAfterPanel.setSize(newd);
    myAfterPanel.setPreferredSize(newd);
    myAfterPanel.setMaximumSize(newd);
    myAfterPanel.setMinimumSize(newd);
  }
}