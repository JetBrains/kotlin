// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.compiler.CompilerSettingsFactory;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CompilerConfigurable implements SearchableConfigurable.Parent, Configurable.NoScroll {

  private final Project myProject;
  private final CompilerUIConfigurable myCompilerUIConfigurable;
  private Configurable[] myKids;

  public CompilerConfigurable(Project project) {
    myProject = project;
    myCompilerUIConfigurable = new CompilerUIConfigurable(myProject);
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "project.propCompiler";
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  public JComponent createComponent() {
    return myCompilerUIConfigurable.createComponent();
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @Override
  public boolean isModified() {
    return myCompilerUIConfigurable.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerUIConfigurable.apply();
  }

  @Override
  public void reset() {
    myCompilerUIConfigurable.reset();
  }

  @Override
  public void disposeUIResources() {
    myCompilerUIConfigurable.disposeUIResources();
  }

  @NotNull
  @Override
  public Configurable[] getConfigurables() {
    if (myKids == null) {
      final CompilerSettingsFactory[] factories = CompilerSettingsFactory.EP_NAME.getExtensions(myProject);
      myKids = ContainerUtil.mapNotNull(factories,
                                        (NullableFunction<CompilerSettingsFactory, Configurable>)factory -> factory.create(myProject), new Configurable[0]);
    }

    return myKids;
  }
}
