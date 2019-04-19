// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options;

import com.intellij.ConfigurableFactory;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModelListener;
import com.intellij.application.options.codeStyle.CodeStyleSchemesPanel;
import com.intellij.application.options.codeStyle.group.CodeStyleGroupProvider;
import com.intellij.application.options.codeStyle.group.CodeStyleGroupProviderFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CodeStyleSchemesConfigurable extends SearchableConfigurable.Parent.Abstract
  implements Configurable.NoMargin, Configurable.NoScroll, Configurable.VariableProjectAppLevel {

  private CodeStyleSchemesPanel myRootSchemesPanel;
  private @NotNull final CodeStyleSchemesModel myModel;
  private List<Configurable> myPanels;
  private boolean myResetCompleted = false;
  private boolean myInitResetInvoked = false;
  private boolean myRevertCompleted = false;

  private final Project myProject;

  public CodeStyleSchemesConfigurable(Project project) {
    myProject = project;
    myModel = new CodeStyleSchemesModel(project);
  }

  @Override
  public JComponent createComponent() {
    initSchemesPanel(myModel);
    return myPanels == null || myPanels.isEmpty() ? null : myPanels.get(0).createComponent();
  }

  private void initSchemesPanel(@NotNull final CodeStyleSchemesModel model) {
    myRootSchemesPanel = new CodeStyleSchemesPanel(model, 0);

    model.addListener(new CodeStyleSchemesModelListener() {
      @Override
      public void currentSchemeChanged(final Object source) {
        if (source != myRootSchemesPanel) {
          myRootSchemesPanel.onSelectedSchemeChanged();
        }
      }

      @Override
      public void schemeListChanged() {
        myRootSchemesPanel.resetSchemesCombo();
      }

      @Override
      public void schemeChanged(final CodeStyleScheme scheme) {
        if (scheme == model.getSelectedScheme()) myRootSchemesPanel.onSelectedSchemeChanged();
      }
    });
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @Override
  public void disposeUIResources() {
    if (myPanels != null) {
      try {
        super.disposeUIResources();
        for (Configurable panel : myPanels) {
          panel.disposeUIResources();
        }
      }
      finally {
        myPanels = null;
        myRootSchemesPanel = null;
        myResetCompleted = false;
        myRevertCompleted = false;
        myInitResetInvoked = false;
      }
    }
  }

  @Override
  public synchronized void reset() {
    if (!myInitResetInvoked) {
      try {
        if (!myResetCompleted) {
          try {
            resetImpl();
          }
          finally {
            myResetCompleted = true;
          }
        }
      }
      finally {
        myInitResetInvoked = true;
      }
    }
    else {
      revert();
    }
  }

  private void resetImpl() {
    myModel.reset();

    if (myPanels != null) {
      for (Configurable panel : myPanels) {
        if (panel instanceof CodeStyleConfigurableWrapper) {
          ((CodeStyleConfigurableWrapper)panel).resetPanel();
        }
        else {
          panel.reset();
        }
      }
    }
  }

  public synchronized void resetFromChild() {
    if (!myResetCompleted) {
      try {
        resetImpl();
      }
      finally {
        myResetCompleted = true;
      }
    }
  }

  public void revert() {
    if (myModel.isSchemeListModified() || isSomeSchemeModified()) {
      myRevertCompleted = false;
    }
    if (!myRevertCompleted) {
      try {
        resetImpl();
      }
      finally {
        myRevertCompleted = true;
      }
    }
  }

  private boolean isSomeSchemeModified() {
    if (myPanels != null) {
      for (Configurable panel : myPanels) {
        if (panel instanceof CodeStyleConfigurableWrapper) {
          if (((CodeStyleConfigurableWrapper)panel).isPanelModified()) return true;
        }
      }
    }

    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    myModel.apply();

    for (Configurable panel : myPanels) {
      if (panel instanceof CodeStyleConfigurableWrapper) {
        ((CodeStyleConfigurableWrapper)panel).applyPanel();
      }
      else {
        panel.apply();
      }
    }

    CodeStyleSettingsManager.getInstance(myProject).fireCodeStyleSettingsChanged(null);
  }

  @Override
  protected Configurable[] buildConfigurables() {
    CodeStyleGroupProviderFactory groupProviderFactory = new CodeStyleGroupProviderFactory(getModel(), this);
    myPanels = new ArrayList<>();
    Set<CodeStyleGroupProvider> addedGroupProviders = ContainerUtil.newHashSet();

    final List<CodeStyleSettingsProvider> providers = ContainerUtil.newArrayList();
    providers.addAll(CodeStyleSettingsProvider.EXTENSION_POINT_NAME.getExtensionList());
    providers.addAll(LanguageCodeStyleSettingsProvider.getSettingsPagesProviders());

    providers.sort((p1, p2) -> {
      if (!p1.getPriority().equals(p2.getPriority())) {
        return p1.getPriority().compareTo(p2.getPriority());
      }
      String name1 = p1.getConfigurableDisplayName();
      if (name1 == null) name1 = "";
      String name2 = p2.getConfigurableDisplayName();
      if (name2 == null) name2 = "";
      return name1.compareToIgnoreCase(name2);
    });

    for (final CodeStyleSettingsProvider provider : providers) {
      if (provider.getGroup() != null) {
        CodeStyleGroupProvider groupProvider = groupProviderFactory.getGroupProvider(provider.getGroup());
        if (!addedGroupProviders.contains(groupProvider)) {
          myPanels.add(groupProvider.createConfigurable());
          addedGroupProviders.add(groupProvider);
        }
        groupProvider.addChildProvider(provider);
      }
      else {
        if (provider.hasSettingsPage()) {
          CodeStyleConfigurableWrapper e =
            ConfigurableFactory.Companion.getInstance().createCodeStyleConfigurable(provider, getModel(), this);
          myPanels.add(e);
        }
      }
    }

    int size = myPanels.size();
    Configurable[] result = new Configurable[size > 0 ? size - 1 : 0];
    for (int i = 0; i < result.length; i++) {
      result[i] = myPanels.get(i + 1);
    }
    return result;
  }

  void resetCompleted() {
    myRevertCompleted = false;
  }

  @NotNull
  CodeStyleSchemesModel getModel() {
    return myModel;
  }

  @Override
  public String getDisplayName() {
    return "Code Style";
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.globalcodestyle";
  }

  @Override
  public boolean isModified() {
    if (myModel.containsModifiedCodeStyleSettings()) return true;
    for (Configurable panel : myPanels) {
      if (panel.isModified()) return true;
    }
    boolean schemeListModified = myModel.isSchemeListModified();
    if (schemeListModified) {
      myRevertCompleted = false;
    }
    return schemeListModified;
  }

  @Override
  @NotNull
  public String getId() {
    return "preferences.sourceCode";
  }

  @Override
  public boolean isProjectLevel() {
    return myModel.isUsePerProjectSettings();
  }

  @Nullable
  public SearchableConfigurable findSubConfigurable(@NotNull final String name) {
    return findSubConfigurable(this, name);
  }

  private static SearchableConfigurable findSubConfigurable(SearchableConfigurable.Parent topConfigurable, @NotNull final String name) {
    for (Configurable configurable : topConfigurable.getConfigurables()) {
      if (configurable instanceof SearchableConfigurable) {
        if (name.equals(configurable.getDisplayName())) return (SearchableConfigurable)configurable;
        if (configurable instanceof SearchableConfigurable.Parent) {
          SearchableConfigurable child = findSubConfigurable((Parent)configurable, name);
          if (child != null) return child;
        }
      }
    }
    return null;
  }
}
