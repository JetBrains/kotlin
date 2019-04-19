// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

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
  private final BackendCompiler myDefaultCompiler;
  private BackendCompiler mySelectedCompiler;
  private final CompilerConfigurationImpl myCompilerConfiguration;
  private final TargetOptionsComponent myTargetLevelComponent;

  public JavaCompilersTab(final Project project) {
    this(project, ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(project)).getDefaultCompiler());
  }

  private JavaCompilersTab(final Project project, BackendCompiler defaultCompiler) {
    myProject = project;
    myDefaultCompiler = defaultCompiler;
    myCompilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(project);

    myCardLayout = new CardLayout();
    myContentPanel.setLayout(myCardLayout);

    myTargetOptionsPanel.setLayout(new BorderLayout());
    myTargetLevelComponent = new TargetOptionsComponent(project);
    myTargetOptionsPanel.add(myTargetLevelComponent, BorderLayout.CENTER);

    final List<BackendCompiler> compilers = getCompilers();
    final List<Configurable> configurables = getConfigurables();
    for (int i = 0; i < configurables.size(); i++) {
      myContentPanel.add(configurables.get(i).createComponent(), compilers.get(i).getId());
    }
    myCompiler.setModel(new DefaultComboBoxModel<>(new Vector<>(compilers)));
    myCompiler.setRenderer(new ListCellRendererWrapper<BackendCompiler>() {
      @Override
      public void customize(final JList list, final BackendCompiler value, final int index, final boolean selected, final boolean hasFocus) {
        setText(value != null ? value.getPresentableName() : "");
      }
    });
    myCompiler.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final BackendCompiler compiler = (BackendCompiler)myCompiler.getSelectedItem();
        if (compiler != null) {
          selectCompiler(compiler);
        }
      }
    });
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("java.compiler.description");
  }

  @Override
  public String getHelpTopic() {
    return "reference.projectsettings.compiler.javacompiler";
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    if (!Comparing.equal(mySelectedCompiler, myCompilerConfiguration.getDefaultCompiler())) {
      return true;
    }
    if (myCbUseReleaseOption.isSelected() != myCompilerConfiguration.useReleaseOption()) {
      return true;
    }
    if (super.isModified()) {
      return true;
    }
    if (!Comparing.equal(myTargetLevelComponent.getProjectBytecodeTarget(), myCompilerConfiguration.getProjectBytecodeTarget())) {
      return true;
    }
    if (!Comparing.equal(myTargetLevelComponent.getModulesBytecodeTargetMap(), myCompilerConfiguration.getModulesBytecodeTargetMap())) {
      return true;
    }
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    try {
      myCompilerConfiguration.setUseReleaseOption(myCbUseReleaseOption.isSelected());

      super.apply();

      myCompilerConfiguration.setDefaultCompiler(mySelectedCompiler);
      myCompilerConfiguration.setProjectBytecodeTarget(myTargetLevelComponent.getProjectBytecodeTarget());
      myCompilerConfiguration.setModulesBytecodeTargetMap(myTargetLevelComponent.getModulesBytecodeTargetMap());

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
    myCbUseReleaseOption.setSelected(myCompilerConfiguration.useReleaseOption());

    super.reset();

    selectCompiler(myCompilerConfiguration.getDefaultCompiler());
    myTargetLevelComponent.setProjectBytecodeTargetLevel(myCompilerConfiguration.getProjectBytecodeTarget());
    myTargetLevelComponent.setModuleTargetLevels(myCompilerConfiguration.getModulesBytecodeTargetMap());
  }

  private void selectCompiler(BackendCompiler compiler) {
    if(compiler == null) {
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
    final Collection<BackendCompiler> compilers = getCompilers();
    final List<Configurable> configurables = new ArrayList<>(compilers.size());
    for (final BackendCompiler compiler : compilers) {
      configurables.add(compiler.createConfigurable());
    }
    return configurables;
  }

  @NotNull
  private List<BackendCompiler> getCompilers() {
    final CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    return (List<BackendCompiler>)configuration.getRegisteredJavaCompilers();
  }
}
