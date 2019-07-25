// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.javaCompiler.javac;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;

import java.util.HashMap;
import java.util.Map;

@State(name = "JavacSettings", storages = @Storage("compiler.xml"))
public class JavacConfiguration implements PersistentStateComponent<JpsJavaCompilerOptions> {
  private final JpsJavaCompilerOptions mySettings = new JpsJavaCompilerOptions();
  private final Project myProject;

  public JavacConfiguration(Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public JpsJavaCompilerOptions getState() {
    final JpsJavaCompilerOptions state = new JpsJavaCompilerOptions();
    XmlSerializerUtil.copyBean(mySettings, state);
    state.ADDITIONAL_OPTIONS_OVERRIDE = new HashMap<>(state.ADDITIONAL_OPTIONS_OVERRIDE); // copyBean copies by reference, we need a map clone here
    final PathMacroManager macros = PathMacroManager.getInstance(myProject);
    state.ADDITIONAL_OPTIONS_STRING = macros.collapsePathsRecursively(state.ADDITIONAL_OPTIONS_STRING);
    for (Map.Entry<String, String> entry : state.ADDITIONAL_OPTIONS_OVERRIDE.entrySet()) {
      entry.setValue(macros.collapsePathsRecursively(entry.getValue()));
    }
    return state;
  }

  @Override
  public void loadState(@NotNull JpsJavaCompilerOptions state) {
    XmlSerializerUtil.copyBean(state, mySettings);
  }

  public static JpsJavaCompilerOptions getOptions(Project project, Class<? extends JavacConfiguration> aClass) {
    JavacConfiguration configuration = ServiceManager.getService(project, aClass);
    return configuration.mySettings;
  }
}