/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler.impl.javaCompiler.eclipse;

import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.impl.java.EclipseCompilerTool;
import org.jetbrains.jps.model.java.compiler.CompilerOptions;
import org.jetbrains.jps.model.java.compiler.JavaCompilers;

import java.util.Collections;
import java.util.Set;

public class EclipseCompiler implements BackendCompiler {
  private final Project myProject;

  public EclipseCompiler(Project project) {
    myProject = project;
  }

  public static boolean isInitialized() {
    return EclipseCompilerTool.findEcjJarFile() != null;
  }

  @Override
  @NotNull
  public String getId() { // used for externalization
    return JavaCompilers.ECLIPSE_ID;
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return CompilerBundle.message("compiler.eclipse.name");
  }

  @Override
  @NotNull
  public Configurable createConfigurable() {
    return new EclipseCompilerConfigurable(myProject, EclipseCompilerConfiguration.getOptions(myProject, EclipseCompilerConfiguration.class));
  }

  @NotNull
  @Override
  public Set<FileType> getCompilableFileTypes() {
    return Collections.singleton(StdFileTypes.JAVA);
  }

  @NotNull
  @Override
  public CompilerOptions getOptions() {
    return EclipseCompilerConfiguration.getOptions(myProject, EclipseCompilerConfiguration.class);
  }
}
