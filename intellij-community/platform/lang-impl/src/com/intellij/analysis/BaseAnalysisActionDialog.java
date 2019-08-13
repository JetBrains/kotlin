/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis;

import com.intellij.analysis.dialog.*;
import com.intellij.find.FindSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.util.RadioUpDownListener;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BaseAnalysisActionDialog extends DialogWrapper {
  private final static Logger LOG = Logger.getInstance(BaseAnalysisActionDialog.class);

  @NotNull private final AnalysisUIOptions myOptions;
  private final boolean myRememberScope;
  private final boolean myShowInspectTestSource;
  private final String myAnalysisNoon;
  private final Project myProject;
  private final ButtonGroup myGroup = new ButtonGroup();
  private final JCheckBox myInspectTestSource = new JCheckBox();
  private final List<ModelScopeItemView> myViewItems;

  /**
   * @deprecated Use {@link BaseAnalysisActionDialog#BaseAnalysisActionDialog(String, String, Project, List, AnalysisUIOptions, boolean, boolean)} instead.
   */
  @Deprecated
  public BaseAnalysisActionDialog(@NotNull String title,
                                   @NotNull String analysisNoon,
                                   @NotNull Project project,
                                   @NotNull final AnalysisScope scope,
                                   final String moduleName,
                                   final boolean rememberScope,
                                   @NotNull AnalysisUIOptions analysisUIOptions,
                                   @Nullable PsiElement context) {
    this(title, analysisNoon, project, standardItems(project, scope, moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null, context),
         analysisUIOptions, rememberScope);
  }

  @NotNull
  public static List<ModelScopeItem> standardItems(@NotNull Project project,
                                                   @NotNull AnalysisScope scope,
                                                   @Nullable Module module,
                                                   @Nullable PsiElement context) {
    return Stream.of(new ProjectScopeItem(project),
                     new CustomScopeItem(project, context),
                     VcsScopeItem.createIfHasVCS(project),
                     ModuleScopeItem.tryCreate(module),
                     OtherScopeItem.tryCreate(scope)).filter(x -> x != null).collect(Collectors.toList());
  }

  public BaseAnalysisActionDialog(@NotNull String title,
                                @NotNull String analysisNoon,
                                @NotNull Project project,
                                @NotNull List<? extends ModelScopeItem> items,
                                @NotNull AnalysisUIOptions options,
                                final boolean rememberScope) {
    this(title, analysisNoon, project, items, options, rememberScope, ModuleUtil.hasTestSourceRoots(project));
  }

  public BaseAnalysisActionDialog(@NotNull String title,
                                  @NotNull String analysisNoon,
                                  @NotNull Project project,
                                  @NotNull List<? extends ModelScopeItem> items,
                                  @NotNull AnalysisUIOptions options,
                                  final boolean rememberScope,
                                  final boolean showInspectTestSource) {
    super(true);
    myAnalysisNoon = analysisNoon;
    myProject = project;

    myViewItems = ModelScopeItemPresenter.createOrderedViews(items);
    myOptions = options;
    myRememberScope = rememberScope;
    myShowInspectTestSource = showInspectTestSource;

    init();
    setTitle(title);
  }

  @Override
  protected JComponent createCenterPanel() {
    BorderLayoutPanel panel = new BorderLayoutPanel();
    TitledSeparator titledSeparator = new TitledSeparator();
    titledSeparator.setText(myAnalysisNoon);
    panel.addToTop(titledSeparator);

    JPanel scopesPanel = new JPanel(new GridBagLayout());
    panel.addToCenter(scopesPanel);

    int maxColumns = myViewItems.stream()
                       .mapToInt(x -> x.additionalComponents.size())
                       .max().orElse(0) + 1;

    int gridY = 0;
    JRadioButton[] buttons = new JRadioButton[myViewItems.size()];
    GridBagConstraints gbc = new GridBagConstraints();
    for (ModelScopeItemView x: myViewItems) {
      JRadioButton button = x.button;
      List<JComponent> components = x.additionalComponents;

      int gridX = 0;
      buttons[gridY] = button;
      myGroup.add(button);
      int countExtraColumns = components.size();

      gbc.gridy = gridY;
      gbc.gridx = gridX;
      gbc.gridwidth = countExtraColumns == 0 ? maxColumns : 1;
      gbc.weightx = 0.0D;
      gbc.fill = 0;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.insets = JBUI.insetsLeft(10);
      scopesPanel.add(button, gbc);
      gridX++;

      for (JComponent c : components) {
        if (c instanceof Disposable) {
          Disposer.register(myDisposable, (Disposable)c);
        }
        gbc.gridy = gridY;
        gbc.gridx = gridX;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = JBUI.insetsLeft(5);
        scopesPanel.add(c, gbc);
        gridX++;
      }
      gridY++;
    }

    myInspectTestSource.setText(AnalysisScopeBundle.message("scope.option.include.test.sources"));
    myInspectTestSource.setSelected(myOptions.ANALYZE_TEST_SOURCES);
    myInspectTestSource.setVisible(myShowInspectTestSource);
    gbc.gridy = gridY;
    gbc.gridx = 0;
    gbc.gridwidth = maxColumns;
    gbc.weightx = 1.0;
    gbc.fill = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = JBUI.insetsLeft(10);
    scopesPanel.add(myInspectTestSource, gbc);

    preselectButton();

    BorderLayoutPanel wholePanel = new BorderLayoutPanel();
    wholePanel.addToTop(panel);
    final JComponent additionalPanel = getAdditionalActionSettings(myProject);
    if (additionalPanel != null) {
      wholePanel.addToCenter(additionalPanel);
    }
    new RadioUpDownListener(buttons);

    return wholePanel;
  }

  private void preselectButton() {
    if (myRememberScope) {
      int type = myOptions.SCOPE_TYPE;
      List<ModelScopeItemView> preselectedScopes = ContainerUtil.filter(myViewItems, x -> x.scopeId == type);

      if (preselectedScopes.size() >= 1) {
        LOG.assertTrue(preselectedScopes.size() == 1, "preselectedScopes.size() == 1");
        preselectedScopes.get(0).button.setSelected(true);
        return;
      }
    }

    List<ModelScopeItemView> candidates = new ArrayList<>();
    for (ModelScopeItemView view : myViewItems) {
      candidates.add(view);
      if (view.scopeId == AnalysisScope.FILE) {
        break;
      }
    }

    Collections.reverse(candidates);
    for (ModelScopeItemView x : candidates) {
      int scopeType = x.scopeId;
      // skip predefined scopes
      if (scopeType == AnalysisScope.CUSTOM || scopeType == AnalysisScope.UNCOMMITTED_FILES) {
        continue;
      }
      x.button.setSelected(true);
      break;
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    final Enumeration<AbstractButton> enumeration = myGroup.getElements();
    while (enumeration.hasMoreElements()) {
      final AbstractButton button = enumeration.nextElement();
      if (button.isSelected()) {
        return button;
      }
    }
    return super.getPreferredFocusedComponent();
  }

  /**
   * @deprecated Use {@link BaseAnalysisActionDialog#getScope(AnalysisScope)} instead.
   */
  @Deprecated
  public AnalysisScope getScope(@NotNull AnalysisUIOptions uiOptions, @NotNull AnalysisScope defaultScope, @NotNull Project project, Module module) {
    return getScope(defaultScope);
  }

  public boolean isProjectScopeSelected() {
    return myViewItems.stream()
      .filter(x -> x.scopeId == AnalysisScope.PROJECT)
      .findFirst().map(x -> x.button.isSelected()).orElse(false);
  }

  public boolean isModuleScopeSelected() {
    return myViewItems.stream()
      .filter(x -> x.scopeId == AnalysisScope.MODULE)
      .findFirst().map(x -> x.button.isSelected()).orElse(false);
  }

  public boolean isUncommittedFilesSelected(){
    return myViewItems.stream()
      .filter(x -> x.scopeId == AnalysisScope.UNCOMMITTED_FILES)
      .findFirst().map(x -> x.button.isSelected()).orElse(false);
  }

  @Nullable
  public SearchScope getCustomScope(){
    return myViewItems.stream()
      .filter(x -> x.scopeId == AnalysisScope.CUSTOM && x.button.isSelected())
      .findFirst().map(x -> x.model.getScope().toSearchScope()).orElse(null);
  }

  public boolean isInspectTestSources() {
    return myInspectTestSource.isSelected();
  }

  public AnalysisScope getScope(@NotNull AnalysisScope defaultScope) {
    AnalysisScope scope = null;
    for (ModelScopeItemView x : myViewItems) {
      if (x.button.isSelected()) {
        int type = x.scopeId;
        scope = x.model.getScope();
        if (myRememberScope) {
          myOptions.SCOPE_TYPE = type;
          if (type == AnalysisScope.CUSTOM) {
            myOptions.CUSTOM_SCOPE_NAME = scope.toSearchScope().getDisplayName();
          }
        }
      }
    }
    if (scope == null) {
      scope = defaultScope;
      if (myRememberScope) {
        myOptions.SCOPE_TYPE = scope.getScopeType();
      }
    }

    if (myInspectTestSource.isVisible()) {
      if (myRememberScope) {
        myOptions.ANALYZE_TEST_SOURCES = isInspectTestSources();
      }
      scope.setIncludeTestSource(isInspectTestSources());
    }

    FindSettings.getInstance().setDefaultScopeName(scope.getDisplayName());
    return scope;
  }

  @Nullable
  protected JComponent getAdditionalActionSettings(final Project project) {
    return null;
  }
}
