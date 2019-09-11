// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AnnotationProcessingConfigImpl implements AnnotationProcessingConfig, Serializable {
  private final Set<String> myPaths;
  private final List<String> myArgs;

  public AnnotationProcessingConfigImpl(Set<File> files, List<String> args) {
    Set<String> paths = new LinkedHashSet<String>(files.size());
    for (File file : files) {
      paths.add(file.getAbsolutePath());
    }
    myPaths = paths;
    myArgs = args;
  }

  @NotNull
  @Override
  public Collection<String> getAnnotationProcessorPath() {
    return myPaths;
  }

  @NotNull
  @Override
  public Collection<String> getAnnotationProcessorArguments() {
    return myArgs;
  }
}
