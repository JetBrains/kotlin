// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.intention.impl.config.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.HintHint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;

class PostfixDescriptionPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionDescriptionPanel.class);
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;
  private JBLabel myDescriptionLabel;
  private JBLabel myBeforeLabel;
  private JBLabel myAfterLabel;

  PostfixDescriptionPanel() {
    myDescriptionBrowser.setMargin(JBUI.insets(5));
    initializeExamplePanel(myAfterPanel);
    initializeExamplePanel(myBeforePanel);

    myDescriptionLabel.setBorder(JBUI.Borders.emptyBottom(3));
    myBeforeLabel.setBorder(JBUI.Borders.empty(7, 0, 3, 0));
    myAfterLabel.setBorder(JBUI.Borders.empty(7, 0, 3, 0));
  }

  public void reset(@NotNull BeforeAfterMetaData actionMetaData) {
    boolean isEmpty = actionMetaData == PostfixTemplateMetaData.EMPTY_METADATA;
    readHtml(actionMetaData, isEmpty);
    showUsages(myBeforePanel, isEmpty
                              ? new PlainTextDescriptor(CodeInsightBundle.message("templates.postfix.settings.category.before"),
                                                        "before.txt.template")
                              : ArrayUtil.getFirstElement(actionMetaData.getExampleUsagesBefore()));
    showUsages(myAfterPanel, isEmpty
                             ? new PlainTextDescriptor(CodeInsightBundle.message("templates.postfix.settings.category.after"),
                                                       "after.txt.template")
                             : ArrayUtil.getFirstElement(actionMetaData.getExampleUsagesAfter()));
  }

  private void readHtml(@NotNull BeforeAfterMetaData actionMetaData, boolean isEmpty) {
    final HintHint hintHint = new HintHint(myDescriptionBrowser, new Point(0, 0));
    hintHint.setFont(StartupUiUtil.getLabelFont());
    String description = isEmpty ? CodeInsightBundle.message("templates.postfix.settings.category.text")
                                 : getDescription(actionMetaData.getDescription());
    try {
      myDescriptionBrowser.read(new StringReader(HintUtil.prepareHintText(description, hintHint)), null);
    }
    catch (IOException ignore) {
    }
  }

  @NotNull
  private static String getDescription(TextDescriptor url) {
    try {
      return url.getText();
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return "";
  }

  private static void showUsages(@NotNull JPanel panel, @Nullable TextDescriptor exampleUsage) {
    String text = "";
    FileType fileType = PlainTextFileType.INSTANCE;
    if (exampleUsage != null) {
      try {
        text = exampleUsage.getText();
        String name = exampleUsage.getFileName();
        FileTypeManagerEx fileTypeManager = FileTypeManagerEx.getInstanceEx();
        String extension = fileTypeManager.getExtension(name);
        fileType = fileTypeManager.getFileTypeByExtension(extension);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    ((ActionUsagePanel)panel.getComponent(0)).reset(text, fileType);
    panel.repaint();
  }

  private void initializeExamplePanel(@NotNull JPanel panel) {
    panel.setLayout(new BorderLayout());
    ActionUsagePanel actionUsagePanel = new ActionUsagePanel();
    panel.add(actionUsagePanel);
    Disposer.register(this, actionUsagePanel);
  }

  synchronized JPanel getComponent() {
    return myPanel;
  }

  @Override
  public void dispose() {
  }

  public void resetHeights(int preferredWidth) {
    //adjust vertical dimension to be equal for all three panels
    double height =
      (myDescriptionBrowser.getSize().getHeight() + myBeforePanel.getSize().getHeight() + myAfterPanel.getSize().getHeight()) / 3;
    if (height == 0) return;
    final Dimension newd = new Dimension(preferredWidth, (int)height);
    myDescriptionBrowser.setSize(newd);
    myDescriptionBrowser.setPreferredSize(newd);
    myDescriptionBrowser.setMaximumSize(newd);
    myDescriptionBrowser.setMinimumSize(newd);

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
