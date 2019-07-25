// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalTask;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public interface CompilationDetails extends Serializable {
  ExternalTask getCompileTask();

  @Nullable
  File getCompilerExecutable();

  File getCompileWorkingDir();

  @NotNull
  List<File> getFrameworkSearchPaths();

  @NotNull
  List<File> getSystemHeaderSearchPaths();

  @NotNull
  List<File> getUserHeaderSearchPaths();

  @NotNull
  Set<? extends SourceFile> getSources();

  @NotNull
  Set<File> getHeaderDirs();

  @NotNull
  Set<? extends MacroDirective> getMacroDefines();

  @NotNull
  Set<String> getMacroUndefines();

  @NotNull
  List<String> getAdditionalArgs();
}
