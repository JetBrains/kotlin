// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.compiler.CompilerOptions;

import java.util.Set;

public interface BackendCompiler {
  ExtensionPointName<BackendCompiler> EP_NAME = ExtensionPointName.create("com.intellij.java.compiler");

  CompilerOptions EMPTY_OPTIONS = new CompilerOptions() { };

  @NotNull
  String getId(); // used for externalization

  @NotNull
  String getPresentableName();

  @NotNull
  Configurable createConfigurable();

  @NotNull
  Set<FileType> getCompilableFileTypes();

  @NotNull
  default CompilerOptions getOptions() {
    return EMPTY_OPTIONS;
  }
}