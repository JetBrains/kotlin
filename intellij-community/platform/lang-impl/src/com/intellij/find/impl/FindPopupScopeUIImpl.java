// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.ide.util.scopeChooser.ScopeDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.Functions;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;

class FindPopupScopeUIImpl implements FindPopupScopeUI {
  static final ScopeType PROJECT = new ScopeType("Project", FindBundle.message("find.popup.scope.project"), EmptyIcon.ICON_0);
  static final ScopeType MODULE = new ScopeType("Module", FindBundle.message("find.popup.scope.module"), EmptyIcon.ICON_0);
  static final ScopeType DIRECTORY = new ScopeType("Directory", FindBundle.message("find.popup.scope.directory"), EmptyIcon.ICON_0);
  static final ScopeType SCOPE = new ScopeType("Scope", FindBundle.message("find.popup.scope.scope"), EmptyIcon.ICON_0);

  @NotNull private final FindUIHelper myHelper;
  @NotNull private final Project myProject;
  @NotNull private final FindPopupPanel myFindPopupPanel;
  @NotNull private final Pair<ScopeType, JComponent>[] myComponents;

  private ComboBox<String> myModuleComboBox;
  private FindPopupDirectoryChooser myDirectoryChooser;
  private ScopeChooserCombo myScopeCombo;

  FindPopupScopeUIImpl(@NotNull FindPopupPanel panel) {
    myHelper = panel.getHelper();
    myProject = panel.getProject();
    myFindPopupPanel = panel;
    initComponents();

    boolean fullVersion = !PlatformUtils.isDataGrip();
    myComponents =
      fullVersion
      ? ContainerUtil.ar(new Pair<>(PROJECT, new JLabel()),
                         new Pair<>(MODULE, shrink(myModuleComboBox)),
                         new Pair<>(DIRECTORY, myDirectoryChooser),
                         new Pair<>(SCOPE, shrink(myScopeCombo)))
      : ContainerUtil.ar(new Pair<>(SCOPE, shrink(myScopeCombo)),
                         new Pair<>(DIRECTORY, myDirectoryChooser));
  }

  public void initComponents() {
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    String[] names = new String[modules.length];
    for (int i = 0; i < modules.length; i++) {
      names[i] = modules[i].getName();
    }

    Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
    myModuleComboBox = new ComboBox<>(names);
    myModuleComboBox.setSwingPopup(false);
    myModuleComboBox.setMinimumAndPreferredWidth(JBUIScale.scale(300)); // as ScopeChooser
    myModuleComboBox.setRenderer(SimpleListCellRenderer.create("", Functions.id()));

    ActionListener restartSearchListener = e -> scheduleResultsUpdate();
    myModuleComboBox.addActionListener(restartSearchListener);

    myDirectoryChooser = new FindPopupDirectoryChooser(myFindPopupPanel);

    myScopeCombo = new ScopeChooserCombo();
    Object selection = ObjectUtils.coalesce(myHelper.getModel().getCustomScope(),
                                            myHelper.getModel().getCustomScopeName(),
                                            FindSettings.getInstance().getDefaultScopeName());
    myScopeCombo.init(myProject, true, true, selection, new Condition<ScopeDescriptor>() {
      //final String projectFilesScopeName = PsiBundle.message("psi.search.scope.project");
      final String moduleFilesScopeName;

      {
        String moduleScopeName = PsiBundle.message("search.scope.module", "");
        final int ind = moduleScopeName.indexOf(' ');
        moduleFilesScopeName = moduleScopeName.substring(0, ind + 1);
      }

      @Override
      public boolean value(ScopeDescriptor descriptor) {
        final String display = descriptor.getDisplayName();
        return /*!projectFilesScopeName.equals(display) &&*/ !display.startsWith(moduleFilesScopeName);
      }
    });
    myScopeCombo.setBrowseListener(new ScopeChooserCombo.BrowseListener() {

      private FindModel myModelSnapshot;

      @Override
      public void onBeforeBrowseStarted() {
        myModelSnapshot = myHelper.getModel();
        myFindPopupPanel.getCanClose().set(false);
      }

      @Override
      public void onAfterBrowseFinished() {
        if (myModelSnapshot != null) {
          SearchScope scope = myScopeCombo.getSelectedScope();
          if (scope != null) {
            myModelSnapshot.setCustomScope(scope);
          }
          myFindPopupPanel.getCanClose().set(true);
        }
      }
    });
    myScopeCombo.getComboBox().addActionListener(restartSearchListener);
    Disposer.register(myFindPopupPanel.getDisposable(), myScopeCombo);
  }

  @NotNull
  @Override
  public Pair<ScopeType, JComponent>[] getComponents() {
    return myComponents;
  }

  @Override
  public void applyTo(@NotNull FindSettings findSettings, @NotNull FindPopupScopeUI.ScopeType selectedScope) {
    findSettings.setDefaultScopeName(myScopeCombo.getSelectedScopeName());
  }

  @Override
  public void applyTo(@NotNull FindModel findModel, @NotNull FindPopupScopeUI.ScopeType selectedScope) {
    if (selectedScope == PROJECT) {
      findModel.setProjectScope(true);
    }
    else if (selectedScope == DIRECTORY) {
      String directory = myDirectoryChooser.getDirectory();
      findModel.setDirectoryName(directory);
    }
    else if (selectedScope == MODULE) {
      findModel.setModuleName((String)myModuleComboBox.getSelectedItem());
    }
    else if (selectedScope == SCOPE) {
      SearchScope selectedCustomScope = myScopeCombo.getSelectedScope();
      String customScopeName = selectedCustomScope == null ? null : selectedCustomScope.getDisplayName();
      findModel.setCustomScopeName(customScopeName);
      findModel.setCustomScope(selectedCustomScope);
      findModel.setCustomScope(true);
    }
  }

  @Nullable
  @Override
  public ValidationInfo validate(@NotNull FindModel model, FindPopupScopeUI.ScopeType selectedScope) {
    if (selectedScope == DIRECTORY) {
      return myDirectoryChooser.validate(model);
    }
    return null;
  }

  @Override
  public boolean hideAllPopups() {
    final JComboBox[] candidates = { myModuleComboBox, myScopeCombo.getComboBox(), myDirectoryChooser.getComboBox() };
    for (JComboBox candidate : candidates) {
      if (candidate.isPopupVisible()) {
        candidate.hidePopup();
        return true;
      }
    }
    return false;
  }

  @NotNull
  @Override
  public ScopeType initByModel(@NotNull FindModel findModel) {
    myDirectoryChooser.initByModel(findModel);

    final String dirName = findModel.getDirectoryName();
    if (!StringUtil.isEmptyOrSpaces(dirName)) {
      VirtualFile dir = LocalFileSystem.getInstance().findFileByPath(dirName);
      if (dir != null) {
        Module module = ModuleUtilCore.findModuleForFile(dir, myProject);
        if (module != null) {
          myModuleComboBox.setSelectedItem(module.getName());
        }
      }
    }

    ScopeType scope = getScope(findModel);
    ScopeType selectedScope = Arrays.stream(myComponents).filter(o -> o.first == scope).findFirst().orElse(null) == null
                              ? myComponents[0].first
                              : scope;
    if (selectedScope == MODULE) {
      myModuleComboBox.setSelectedItem(findModel.getModuleName());
    }
    return selectedScope;
  }

  private static JComponent shrink(JComponent toShrink) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(toShrink, BorderLayout.WEST);
    wrapper.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    return wrapper;
  }

  private void scheduleResultsUpdate() {
    myFindPopupPanel.scheduleResultsUpdate();
  }

  private ScopeType getScope(FindModel model) {
    if (model.isCustomScope()) {
      return SCOPE;
    }
    if (model.isProjectScope()) {
      return PROJECT;
    }
    if (model.getDirectoryName() != null) {
      return DIRECTORY;
    }
    if (model.getModuleName() != null) {
      return MODULE;
    }
    return PROJECT;
  }
}
