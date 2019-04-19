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

package com.intellij.application.options.colors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NewColorAndFontPanel extends JPanel {
  private final ColorSettingsPage mySettingsPage;
  private final SchemesPanel mySchemesPanel;
  private final OptionsPanel myOptionsPanel;
  private final PreviewPanel myPreviewPanel;
  private final String myCategory;
  private final Collection<String> myOptionList;

  public NewColorAndFontPanel(final SchemesPanel schemesPanel,
                              final OptionsPanel optionsPanel,
                              final PreviewPanel previewPanel,
                              final String category, final Collection<String> optionList, final ColorSettingsPage page) {
    super(new BorderLayout(0, 10));
    mySchemesPanel = schemesPanel;
    myOptionsPanel = optionsPanel;
    myPreviewPanel = previewPanel;
    myCategory = category;
    myOptionList = optionList;
    mySettingsPage = page;

    JPanel top = new JPanel(new BorderLayout());

    top.add(mySchemesPanel, BorderLayout.NORTH);
    top.add(myOptionsPanel.getPanel(), BorderLayout.CENTER);

    // We don't want to show non-used preview panel (it's considered to be not in use if it doesn't contain text).
    if (myPreviewPanel.getPanel() != null && (page == null || !StringUtil.isEmptyOrSpaces(page.getDemoText()))) {
      @SuppressWarnings("SuspiciousNameCombination")
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, myPreviewPanel.getPanel());
      splitPane.setBorder(BorderFactory.createEmptyBorder());
      splitPane.setContinuousLayout(true);
      add(splitPane);
      GuiUtils.replaceJSplitPaneWithIDEASplitter(splitPane);
    }
    else {
      add(top, BorderLayout.CENTER);
    }

    previewPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void selectionInPreviewChanged(@NotNull final String typeToSelect) {
        optionsPanel.selectOption(typeToSelect);
      }
    });

    optionsPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void settingsChanged() {
        mySchemesPanel.updateOnCurrentSettingsChange();
        optionsPanel.applyChangesToScheme();
        previewPanel.updateView();
      }

      @Override
      public void selectedOptionChanged(@NotNull final Object selected) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
          myPreviewPanel.blinkSelectedHighlightType(selected);
        }
      }

    });
    mySchemesPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void schemeChanged(@NotNull final Object source) {
        myOptionsPanel.updateOptionsList();
        myPreviewPanel.updateView();
      }
    });

  }

  public static NewColorAndFontPanel create(final PreviewPanel previewPanel, String category, final ColorAndFontOptions options,
                                            Collection<String> optionList, ColorSettingsPage page) {
    final SchemesPanel schemesPanel = new SchemesPanel(options);

    final OptionsPanel optionsPanel = new OptionsPanelImpl(
      options, schemesPanel, category,
      new CustomizedSwitcherPanel(previewPanel, page));

    return new NewColorAndFontPanel(schemesPanel, optionsPanel, previewPanel, category, optionList, page);
  }

  public Runnable showOption(final String option) {
    return myOptionsPanel.showOption(option);
  }

  public void selectOptionByType(final String typeToSelect) {
    myOptionsPanel.selectOption(typeToSelect);
  }

  @NotNull
  public Set<String> processListOptions() {
    if (myOptionList == null) {
      return myOptionsPanel.processListOptions();
    }
    return new HashSet<>(myOptionList);
  }


  public String getDisplayName() {
    return myCategory;
  }

  public void reset(Object source) {
    resetSchemesCombo(source);
  }

  public void disposeUIResources() {
    myPreviewPanel.disposeUIResources();
  }

  public void addSchemesListener(final ColorAndFontSettingsListener schemeListener) {
    mySchemesPanel.addListener(schemeListener);
  }

  private void resetSchemesCombo(Object source) {
    mySchemesPanel.resetSchemesCombo(source);
  }

  public boolean contains(final EditorSchemeAttributeDescriptor descriptor) {
    return descriptor.getGroup().equals(myCategory);
  }

  public JComponent getPanel() {
    return this;
  }

  public void updatePreview() {
    myPreviewPanel.updateView();
  }
  
  public void updateSchemesPanel() {
    mySchemesPanel.updateOnCurrentSettingsChange();
  }

  public void addDescriptionListener(final ColorAndFontSettingsListener listener) {
    myOptionsPanel.addListener(listener);
  }

  public boolean containsFontOptions() {
    return false;
  }

  public ColorSettingsPage getSettingsPage() {
    return mySettingsPage;
  }
  
  public void setEmptyText(@NotNull String text, @NotNull String details) {
    myOptionsPanel.setEmptyText(text, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        notifyAtSchemePanel(details);
      }
    });
  }

  private void notifyAtSchemePanel(@NotNull String message) {
    final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
    Balloon balloon = popupFactory
      .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
      .setHideOnClickOutside(true)
      .setHideOnKeyOutside(true)
      .createBalloon();
    Disposer.register(ApplicationManager.getApplication(), balloon);
    balloon.show(new RelativePoint(
      mySchemesPanel,
      new Point(mySchemesPanel.getWidth() / 10, mySchemesPanel.getHeight())), Balloon.Position.below);
  }
}
