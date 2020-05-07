// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.compiler.CompilerSettingsFactory;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.extensions.BaseExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class CompilerConfigurable implements SearchableConfigurable.Parent, Configurable.NoScroll, Configurable.WithEpDependencies {

  private final Project myProject;
  private final CompilerUIConfigurable myCompilerUIConfigurable;
  private Configurable[] myKids;

  public CompilerConfigurable(Project project) {
    myProject = project;
    myCompilerUIConfigurable = new CompilerUIConfigurable(myProject);
  }

  @Override
  public String getDisplayName() {
    return JavaCompilerBundle.message("compiler.configurable.display.name");
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

  @Override
  public @NotNull Collection<BaseExtensionPointName<?>> getDependencies() {
    return Collections.singleton(CompilerSettingsFactory.EP_NAME);
  }

  @Override
  public Configurable @NotNull [] getConfigurables() {
    Configurable[] kids = myKids;
    if (kids == null) {
      myKids = kids = CompilerSettingsFactory.EP_NAME.extensions(myProject).map(f -> f.create(myProject)).filter(Objects::nonNull).toArray(Configurable[]::new);
    }
    return kids;
  }
}
