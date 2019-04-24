// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class JavaCompilersTab extends CompositeConfigurable<Configurable> implements SearchableConfigurable, Configurable.NoScroll {
  private JPanel myPanel;
  private JPanel myContentPanel;
  private JComboBox<BackendCompiler> myCompiler;
  private JPanel myTargetOptionsPanel;
  private JBCheckBox myCbUseReleaseOption;
  private final CardLayout myCardLayout;

  private final Project myProject;
  private final CompilerConfigurationImpl myCompilerConfiguration;
  private final BackendCompiler myDefaultCompiler;
  private final TargetOptionsComponent myTargetLevelComponent;
  private final List<Configurable> myConfigurables;
  private BackendCompiler mySelectedCompiler;

  public JavaCompilersTab(@NotNull Project project) {
    myProject = project;
    myCompilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(project);
    myDefaultCompiler = myCompilerConfiguration.getDefaultCompiler();
    myTargetLevelComponent = new TargetOptionsComponent(project);

    myCardLayout = new CardLayout();
    myContentPanel.setLayout(myCardLayout);
    myTargetOptionsPanel.setLayout(new BorderLayout());
    myTargetOptionsPanel.add(myTargetLevelComponent, BorderLayout.CENTER);

    Collection<BackendCompiler> compilers = myCompilerConfiguration.getRegisteredJavaCompilers();
    myConfigurables = new ArrayList<>(compilers.size());
    for (BackendCompiler compiler : compilers) {
      Configurable configurable = compiler.createConfigurable();
      myConfigurables.add(configurable);
      JComponent component = configurable.createComponent();
      assert component != null : configurable.getClass();
      myContentPanel.add(component, compiler.getId());
    }

    myCompiler.setModel(new DefaultComboBoxModel<>(compilers.toArray(new BackendCompiler[0])));
    myCompiler.setRenderer(SimpleListCellRenderer.create("", BackendCompiler::getPresentableName));
    myCompiler.addActionListener(e -> {
      BackendCompiler compiler = (BackendCompiler)myCompiler.getSelectedItem();
      if (compiler != null) {
        selectCompiler(compiler);
      }
    });
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("java.compiler.description");
  }

  @NotNull
  @Override
  @SuppressWarnings("SpellCheckingInspection")
  public String getHelpTopic() {
    return "reference.projectsettings.compiler.javacompiler";
  }

  @NotNull
  @Override
  public String getId() {
    return getHelpTopic();
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return !Comparing.equal(mySelectedCompiler, myCompilerConfiguration.getDefaultCompiler()) ||
           myCbUseReleaseOption.isSelected() != myCompilerConfiguration.useReleaseOption() ||
           !Comparing.equal(myTargetLevelComponent.getProjectBytecodeTarget(), myCompilerConfiguration.getProjectBytecodeTarget()) ||
           !Comparing.equal(myTargetLevelComponent.getModulesBytecodeTargetMap(), myCompilerConfiguration.getModulesBytecodeTargetMap()) ||
           super.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    try {
      myCompilerConfiguration.setDefaultCompiler(mySelectedCompiler);
      myCompilerConfiguration.setUseReleaseOption(myCbUseReleaseOption.isSelected());
      myCompilerConfiguration.setProjectBytecodeTarget(myTargetLevelComponent.getProjectBytecodeTarget());
      myCompilerConfiguration.setModulesBytecodeTargetMap(myTargetLevelComponent.getModulesBytecodeTargetMap());

      super.apply();

      myTargetLevelComponent.setProjectBytecodeTargetLevel(myCompilerConfiguration.getProjectBytecodeTarget());
      myTargetLevelComponent.setModuleTargetLevels(myCompilerConfiguration.getModulesBytecodeTargetMap());
    }
    finally {
      BuildManager.getInstance().clearState(myProject);
      PsiManager.getInstance(myProject).dropPsiCaches();
    }
  }

  @Override
  public void reset() {
    super.reset();
    selectCompiler(myCompilerConfiguration.getDefaultCompiler());
    myCbUseReleaseOption.setSelected(myCompilerConfiguration.useReleaseOption());
    myTargetLevelComponent.setProjectBytecodeTargetLevel(myCompilerConfiguration.getProjectBytecodeTarget());
    myTargetLevelComponent.setModuleTargetLevels(myCompilerConfiguration.getModulesBytecodeTargetMap());
  }

  private void selectCompiler(BackendCompiler compiler) {
    if (compiler == null) {
      compiler = myDefaultCompiler;
    }
    myCompiler.setSelectedItem(compiler);
    mySelectedCompiler = compiler;
    myCardLayout.show(myContentPanel, compiler.getId());
    myContentPanel.revalidate();
    myContentPanel.repaint();
  }

  @NotNull
  @Override
  protected List<Configurable> createConfigurables() {
    return myConfigurables;
  }
}