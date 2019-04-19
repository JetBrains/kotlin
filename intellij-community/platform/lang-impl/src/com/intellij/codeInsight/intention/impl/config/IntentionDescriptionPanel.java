// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurableProxy;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.openapi.application.ApplicationInfo;
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
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IntentionDescriptionPanel {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.config.IntentionDescriptionPanel");
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;
  private TitledSeparator myBeforeSeparator;
  private TitledSeparator myAfterSeparator;
  private JPanel myPoweredByPanel;
  private JPanel myDescriptionPanel;
  private final List<IntentionUsagePanel> myBeforeUsagePanels = new ArrayList<>();
  private final List<IntentionUsagePanel> myAfterUsagePanels = new ArrayList<>();
  @NonNls private static final String BEFORE_TEMPLATE = "before.java.template";
  @NonNls private static final String AFTER_TEMPLATE = "after.java.template";

  public IntentionDescriptionPanel() {
    myDescriptionBrowser.addHyperlinkListener(
      new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            BrowserUtil.browse(e.getURL());
          }
        }
      }
    );
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
    hintHint.setFont(UIUtil.getLabelFont());
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

      showUsages(myBeforePanel, myBeforeSeparator, myBeforeUsagePanels, actionMetaData.getExampleUsagesBefore());
      showUsages(myAfterPanel, myAfterSeparator, myAfterUsagePanels, actionMetaData.getExampleUsagesAfter());

      SwingUtilities.invokeLater(() -> myPanel.revalidate());

    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private void setupPoweredByPanel(final IntentionActionMetaData actionMetaData) {
    PluginId pluginId = actionMetaData == null ? null : actionMetaData.getPluginId();
    JComponent owner;
    if (pluginId == null) {
      ApplicationInfo info = ApplicationInfo.getInstance();
      String label = XmlStringUtil.wrapInHtml(info.getVersionName());
      owner = new JLabel(label);
    }
    else {
      IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
      HyperlinkLabel label = new HyperlinkLabel(CodeInsightBundle.message("powered.by.plugin", pluginDescriptor.getName()));
      label.addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          Project project = ProjectManager.getInstance().getDefaultProject();
          PluginManagerConfigurableProxy.showPluginConfigurable(null, project, pluginDescriptor);
        }
      });
      owner = label;
    }
    //myPoweredByContainer.setVisible(true);
    myPoweredByPanel.removeAll();
    myPoweredByPanel.add(owner, BorderLayout.CENTER);
  }


  public void reset(String intentionCategory)  {
    try {
      readHTML(toHTML(CodeInsightBundle.message("intention.settings.category.text", intentionCategory)));
      setupPoweredByPanel(null);

      URL beforeURL = getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.','/') + "/" + BEFORE_TEMPLATE);
      showUsages(myBeforePanel, myBeforeSeparator, myBeforeUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(beforeURL)});
      URL afterURL = getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.','/') + "/" + AFTER_TEMPLATE);
      showUsages(myAfterPanel, myAfterSeparator, myAfterUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(afterURL)});

      SwingUtilities.invokeLater(() -> myPanel.revalidate());
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void showUsages(final JPanel panel,
                                 final TitledSeparator separator,
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
          if (i == exampleUsages.length) {
            gb.gridwidth = GridBagConstraints.REMAINDER;
          }
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