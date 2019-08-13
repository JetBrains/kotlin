// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.contextConfiguration;

import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.codeStyle.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS;
import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS;

class CodeFragmentCodeStyleSettingsPanel extends TabbedLanguageCodeStylePanel {
  private static final Logger LOG = Logger.getInstance(CodeFragmentCodeStyleSettingsPanel.class);

  private final CodeStyleSettingsCodeFragmentFilter.CodeStyleSettingsToShow mySettingsToShow;
  @NotNull private final LanguageCodeStyleSettingsProvider mySettingsProvider;
  private final SelectedTextFormatter mySelectedTextFormatter;
  private SpacesPanelWithoutPreview mySpacesPanel;
  private WrappingAndBracesPanelWithoutPreview myWrappingPanel;

  private Runnable mySomethingChangedCallback;

  CodeFragmentCodeStyleSettingsPanel(@NotNull CodeStyleSettings settings,
                                     @NotNull CodeStyleSettingsCodeFragmentFilter.CodeStyleSettingsToShow settingsToShow,
                                     @NotNull LanguageCodeStyleSettingsProvider settingsProvider,
                                     @NotNull SelectedTextFormatter selectedTextFormatter) {
    super(settingsProvider.getLanguage(), settings, settings.clone());
    mySettingsToShow = settingsToShow;
    mySettingsProvider = settingsProvider;
    mySelectedTextFormatter = selectedTextFormatter;

    ensureTabs();
  }

  public void setOnSomethingChangedCallback(Runnable runnable) {
    mySomethingChangedCallback = runnable;
  }

  @Override
  protected void somethingChanged() {
    if (mySomethingChangedCallback != null) {
      mySomethingChangedCallback.run();
    }
  }

  @Override
  protected String getPreviewText() {
    return null;
  }

  @Override
  protected void updatePreview(boolean useDefaultSample) {
  }

  @Override
  protected void initTabs(CodeStyleSettings settings) {
    SpacesPanelWithoutPreview panel = getSpacesPanel(settings);
    if (panel != null) {
      mySpacesPanel = panel;
      addTab(mySpacesPanel);
    }

    myWrappingPanel = new WrappingAndBracesPanelWithoutPreview(settings);
    addTab(myWrappingPanel);
    reset(getSettings());
  }

  @Nullable
  private SpacesPanelWithoutPreview getSpacesPanel(CodeStyleSettings settings) {
    SpacesPanelWithoutPreview spacesPanel = new SpacesPanelWithoutPreview(settings);
    if (spacesPanel.hasSomethingToShow()) {
      return spacesPanel;
    }
    Disposer.dispose(spacesPanel);
    return null;
  }

  public JComponent getPreferredFocusedComponent() {
    return mySpacesPanel != null ? mySpacesPanel.getPreferredFocusedComponent()
                                 : myWrappingPanel.getPreferredFocusedComponent();
  }

  public static boolean hasOptionsToShow(LanguageCodeStyleSettingsProvider provider) {
    LanguageCodeStyleSettingsProvider.SettingsType[] types = { SPACING_SETTINGS, WRAPPING_AND_BRACES_SETTINGS };
    for (LanguageCodeStyleSettingsProvider.SettingsType type : types) {
      if (!provider.getSupportedFields(type).isEmpty()) {
        return true;
      }
    }

    return !provider.getSupportedFields().isEmpty();
  }

  private void reformatSelectedTextWithNewSettings() {
    try {
      apply(getSettings());
    }
    catch (ConfigurationException e) {
      LOG.debug("Cannot apply code style settings", e);
    }

    CodeStyleSettings clonedSettings = getSettings().clone();
    mySelectedTextFormatter.reformatSelectedText(clonedSettings);
  }

  private class SpacesPanelWithoutPreview extends MySpacesPanel {
    private JPanel myPanel;

    SpacesPanelWithoutPreview(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    protected void somethingChanged() {
      mySelectedTextFormatter.restoreSelectedText();
      reformatSelectedTextWithNewSettings();
      CodeFragmentCodeStyleSettingsPanel.this.somethingChanged();
    }

    @Override
    protected void init() {
      List<String> settingNames = mySettingsToShow.getSettings(getSettingsType());
      if (settingNames.isEmpty()) {
        settingNames = mySettingsToShow.getOtherSetting();
      }

      mySettingsProvider.customizeSettings(getFilteredSettingsConsumer(settingNames, this), getSettingsType());
      initTables();

      myOptionsTree = createOptionsTree();
      myOptionsTree.setCellRenderer(new MyTreeCellRenderer());

      JBScrollPane pane = new JBScrollPane(myOptionsTree) {
        @Override
        public Dimension getMinimumSize() {
          return super.getPreferredSize();
        }
      };

      myPanel = new JPanel(new BorderLayout());
      myPanel.add(pane);

      isFirstUpdate = false;
    }

    public boolean hasSomethingToShow() {
      return !myKeys.isEmpty();
    }

    @Override
    public JComponent getPanel() {
      return myPanel;
    }

    @Override
    protected String getPreviewText() {
      return null;
    }

    public JComponent getPreferredFocusedComponent() {
      return myOptionsTree;
    }
  }

  private class WrappingAndBracesPanelWithoutPreview extends MyWrappingAndBracesPanel {
    public JPanel myPanel;

    WrappingAndBracesPanelWithoutPreview(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    protected void init() {
      Collection<String> settingNames = mySettingsToShow.getSettings(getSettingsType());
      if (settingNames.isEmpty()) {
        settingNames = mySettingsToShow.getOtherSetting();
      }

      initTables();

      Collection<String> fields = populateWithAssociatedFields(settingNames);
      fields.add("KEEP_LINE_BREAKS");

      mySettingsProvider.customizeSettings(getFilteredSettingsConsumer(settingNames, this), getSettingsType());

      myTreeTable = createOptionsTree(getSettings());
      JBScrollPane scrollPane = new JBScrollPane(myTreeTable) {
        @Override
        public Dimension getMinimumSize() {
          return myTreeTable.getPreferredSize();
        }
      };

      myPanel = new JPanel(new BorderLayout());
      myPanel.add(scrollPane);

      isFirstUpdate = false;
    }

    @NotNull
    private Collection<String> populateWithAssociatedFields(Collection<String> settingNames) {
      Set<String> commonFields = new HashSet<>();
      for (String fieldName : settingNames) {
        SettingsGroup settingsGroup = getAssociatedSettingsGroup(fieldName);
        if (settingsGroup == null) {
          commonFields.add(fieldName);
        }
        else if (settingsGroup.title != WRAPPING_KEEP) {
          commonFields.addAll(settingsGroup.commonCodeStyleSettingFieldNames);
        }
      }
      return commonFields;
    }

    @Override
    public JComponent getPanel() {
      return myPanel;
    }

    @Override
    protected void somethingChanged() {
      mySelectedTextFormatter.restoreSelectedText();
      reformatSelectedTextWithNewSettings();
      CodeFragmentCodeStyleSettingsPanel.this.somethingChanged();
    }

    @Override
    protected String getPreviewText() {
      return null;
    }

    public JComponent getPreferredFocusedComponent() {
      return myTreeTable;
    }
  }

  @NotNull
  private static CodeStyleSettingsCustomizable getFilteredSettingsConsumer(@NotNull Collection<String> names, @NotNull CodeStyleSettingsCustomizable original) {
    return new CodeStyleSettingsCustomizable() {
      @Override
      public void showAllStandardOptions() {
        original.showStandardOptions(ArrayUtilRt.toStringArray(names));
      }

      @Override
      public void showStandardOptions(String... optionNames) {
        String[] toShowOptions = Arrays.stream(optionNames).filter(names::contains).toArray(value -> new String[value]);
        original.showStandardOptions(toShowOptions);
      }

      @Override
      public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                                   String fieldName,
                                   String title,
                                   @Nullable String groupName,
                                   Object... options) {
        if (names.contains(fieldName)) {
          original.showCustomOption(settingsClass, fieldName, title, groupName, options);
        }
      }

      @Override
      public void renameStandardOption(String fieldName, String newTitle) {
        if (names.contains(fieldName)) {
          original.renameStandardOption(fieldName, newTitle);
        }
      }

      @Override
      public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                                   String fieldName,
                                   String title,
                                   @Nullable String groupName,
                                   @Nullable OptionAnchor anchor,
                                   @Nullable String anchorFieldName,
                                   Object... options) {
        if (names.contains(fieldName)) {
          original.showCustomOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName, options);
        }
      }
    };
  }
}
