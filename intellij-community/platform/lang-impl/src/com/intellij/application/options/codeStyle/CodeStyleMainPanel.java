// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.codeStyle;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.components.labels.SwingActionLink;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CodeStyleMainPanel extends JPanel implements TabbedLanguageCodeStylePanel.TabChangeListener {
  private final CardLayout myLayout = new CardLayout();
  private final JPanel mySettingsPanel = new JPanel(myLayout);

  private final Map<String, NewCodeStyleSettingsPanel> mySettingsPanels = new HashMap<>();

  private Future<?> myAlarm = CompletableFuture.completedFuture(null);
  private final CodeStyleSchemesModel myModel;
  private final CodeStyleSettingsPanelFactory myFactory;
  private final CodeStyleSchemesPanel mySchemesPanel;
  private boolean myIsDisposed;
  private final Action mySetFromAction = new AbstractAction("Set from...") {
    @Override
    public void actionPerformed(ActionEvent event) {
      CodeStyleAbstractPanel selectedPanel = ensureCurrentPanel().getSelectedPanel();
      if (selectedPanel instanceof TabbedLanguageCodeStylePanel) {
        ((TabbedLanguageCodeStylePanel)selectedPanel).showSetFrom((Component)event.getSource());
      }
    }
  };

  @NonNls
  private static final String WAIT_CARD = "CodeStyleSchemesConfigurable.$$$.Wait.placeholder.$$$";

  private final PropertiesComponent myProperties;

  private static final String SELECTED_TAB = "settings.code.style.selected.tab";

  public CodeStyleMainPanel(CodeStyleSchemesModel model, CodeStyleSettingsPanelFactory factory, boolean schemesPanelEnabled) {
    super(new BorderLayout());
    myModel = model;
    myFactory = factory;
    mySchemesPanel = new CodeStyleSchemesPanel(model, createLinkComponent());
    myProperties = PropertiesComponent.getInstance();

    model.addListener(new CodeStyleSchemesModelListener(){
      @Override
      public void currentSchemeChanged(final Object source) {
        if (source != mySchemesPanel) {
          mySchemesPanel.onSelectedSchemeChanged();
        }
        onCurrentSchemeChanged();
      }

      @Override
      public void schemeListChanged() {
        mySchemesPanel.resetSchemesCombo();
      }

      @Override
      public void beforeCurrentSettingsChanged() {
        if (!myIsDisposed) {
          ensureCurrentPanel().onSomethingChanged();
        }
      }

      @Override
      public void afterCurrentSettingsChanged() {
        mySchemesPanel.updateOnCurrentSettingsChange();
        DataContext context = DataManager.getInstance().getDataContext(mySettingsPanel);
        Settings settings = Settings.KEY.getData(context);
        if (settings != null) {
          settings.revalidate();
        }
      }

      @Override
      public void schemeChanged(final CodeStyleScheme scheme) {
        ensurePanel(scheme).reset(scheme.getCodeStyleSettings());
      }

      @Override
      public void settingsChanged(@NotNull CodeStyleSettings settings) {
        ensureCurrentPanel().reset(settings);
      }

      @Override
      public void overridingStatusChanged() {
        ApplicationManager.getApplication().invokeLater(() -> mySchemesPanel.updateOverridingMessage(), ModalityState.any());
      }
    });

    addWaitCard();

    JPanel top = new JPanel();
    top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

    if (schemesPanelEnabled) {
      top.add(mySchemesPanel);
    }

    top.setBorder(JBUI.Borders.empty(5, 10, 0, 10));
    add(top, BorderLayout.NORTH);
    add(mySettingsPanel, BorderLayout.CENTER);

    mySchemesPanel.resetSchemesCombo();
    mySchemesPanel.onSelectedSchemeChanged();
    onCurrentSchemeChanged();

  }

  @NotNull
  private JComponent createLinkComponent() {
    JPanel linkPanel = new JPanel();
    JLabel link = new SwingActionLink(mySetFromAction);
    link.setVerticalAlignment(SwingConstants.BOTTOM);
    linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.Y_AXIS));
    linkPanel.add(Box.createVerticalGlue());
    linkPanel.add(link);
    return linkPanel;
  }

  private void addWaitCard() {
    JPanel waitPanel = new JPanel(new BorderLayout());
    JLabel label = new JLabel(ApplicationBundle.message("label.loading.page.please.wait"));
    label.setHorizontalAlignment(SwingConstants.CENTER);
    waitPanel.add(label, BorderLayout.CENTER);
    label.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    waitPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    mySettingsPanel.add(WAIT_CARD, waitPanel);
  }

  private void onCurrentSchemeChanged() {
    myLayout.show(mySettingsPanel, WAIT_CARD);
    final Runnable replaceLayout = () -> {
      if (!myIsDisposed) {
        ensureCurrentPanel().onSomethingChanged();
        String schemeName = myModel.getSelectedScheme().getName();
        updateSetFrom();
        myLayout.show(mySettingsPanel, schemeName);
      }
    };
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      replaceLayout.run();
    } else {
      myAlarm.cancel(false);
      myAlarm = EdtExecutorService.getScheduledExecutorInstance().schedule(replaceLayout, 200, TimeUnit.MILLISECONDS);
    }
  }

  private void updateSetFrom() {
    mySetFromAction.setEnabled(ensureCurrentPanel().getSelectedPanel() instanceof TabbedLanguageCodeStylePanel);
  }

  public NewCodeStyleSettingsPanel[] getPanels() {
    final Collection<NewCodeStyleSettingsPanel> panels = mySettingsPanels.values();
    return panels.toArray(new NewCodeStyleSettingsPanel[0]);
  }

  public boolean isModified() {
    if (myModel.isSchemeListModified()) {
      return true;
    }

    final NewCodeStyleSettingsPanel[] panels = getPanels();
    for (NewCodeStyleSettingsPanel panel : panels) {
      //if (!panel.isMultiLanguage()) mySchemesPanel.setPredefinedEnabled(false);
      if (panel.isModified()) return true;
    }
    return false;
  }

  public void reset() {
    clearPanels();
    onCurrentSchemeChanged();
  }

  private void clearPanels() {
    for (NewCodeStyleSettingsPanel panel : mySettingsPanels.values()) {
      panel.dispose();
    }
    mySettingsPanels.clear();
  }

  public void apply() throws ConfigurationException {
    for (NewCodeStyleSettingsPanel panel : getPanels()) {
      if (panel.isModified()) panel.apply();
    }
  }

  @NonNls
  @Nullable
  public String getHelpTopic() {
    NewCodeStyleSettingsPanel selectedPanel = ensureCurrentPanel();
    return selectedPanel != null
           ? selectedPanel.getHelpTopic()
           : "reference.settingsdialog.IDE.globalcodestyle";
  }

  private NewCodeStyleSettingsPanel ensureCurrentPanel() {
    return ensurePanel(myModel.getSelectedScheme());
  }

  public void showTabOnCurrentPanel(String tab) {
    NewCodeStyleSettingsPanel selectedPanel = ensureCurrentPanel();
    CodeStyleAbstractPanel settingsPanel = selectedPanel.getSelectedPanel();
    if (settingsPanel instanceof TabbedLanguageCodeStylePanel) {
      TabbedLanguageCodeStylePanel tabbedPanel = (TabbedLanguageCodeStylePanel)settingsPanel;
      tabbedPanel.changeTab(tab);
    }
  }

  private NewCodeStyleSettingsPanel ensurePanel(final CodeStyleScheme scheme) {
    String name = scheme.getName();
    if (!mySettingsPanels.containsKey(name)) {
      NewCodeStyleSettingsPanel panel = myFactory.createPanel(scheme);
      panel.reset(myModel.getCloneSettings(scheme));
      panel.setModel(myModel);
      CodeStyleAbstractPanel settingsPanel = panel.getSelectedPanel();
      if (settingsPanel instanceof TabbedLanguageCodeStylePanel) {
        TabbedLanguageCodeStylePanel tabbedPanel = (TabbedLanguageCodeStylePanel)settingsPanel;
        tabbedPanel.setListener(this);
        String currentTab = myProperties.getValue(getSelectedTabPropertyName(tabbedPanel));
        if (currentTab != null) {
          tabbedPanel.changeTab(currentTab);
        }
        mySchemesPanel.setSeparatorVisible(false);
      }
      mySettingsPanels.put(name, panel);
      mySettingsPanel.add(scheme.getName(), panel);
    }

    return mySettingsPanels.get(name);
  }

  public String getDisplayName() {
    return myModel.getSelectedScheme().getName();
  }

  public void disposeUIResources() {
    myAlarm.cancel(false);
    clearPanels();
    myIsDisposed = true;
  }

  public boolean isModified(final CodeStyleScheme scheme) {
    if (!mySettingsPanels.containsKey(scheme.getName())) {
      return false;
    }

    return mySettingsPanels.get(scheme.getName()).isModified();
  }

  @NotNull
  public Set<String> processListOptions() {
    final CodeStyleScheme defaultScheme = CodeStyleSchemes.getInstance().getDefaultScheme();
    final NewCodeStyleSettingsPanel panel = ensurePanel(defaultScheme);
    return panel.processListOptions();
  }

  @Override
  public void tabChanged(@NotNull TabbedLanguageCodeStylePanel source, @NotNull String tabTitle) {
    myProperties.setValue(getSelectedTabPropertyName(source), tabTitle);
    for (NewCodeStyleSettingsPanel panel : getPanels()) {
      panel.tabChanged(source, tabTitle);
    }
  }

  @NotNull
  private static String getSelectedTabPropertyName(@NotNull TabbedLanguageCodeStylePanel panel) {
    Language language = panel.getDefaultLanguage();
    return language != null ? SELECTED_TAB + "." + language.getID() : SELECTED_TAB;
  }
}
