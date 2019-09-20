// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.javaCompiler.eclipse;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.compiler.EclipseCompilerOptions;

@State(name = "EclipseCompilerSettings", storages = @Storage("compiler.xml"))
public class EclipseCompilerConfiguration implements PersistentStateComponent<EclipseCompilerOptions> {
  private final EclipseCompilerOptions mySettings = new EclipseCompilerOptions();

  @Override
  @NotNull
  public EclipseCompilerOptions getState() {
    return mySettings;
  }

  @Override
  public void loadState(@NotNull EclipseCompilerOptions state) {
    XmlSerializerUtil.copyBean(state, mySettings);
  }

  public static EclipseCompilerOptions getOptions(Project project, Class<? extends EclipseCompilerConfiguration> aClass) {
    return ServiceManager.getService(project, aClass).getState();
  }}