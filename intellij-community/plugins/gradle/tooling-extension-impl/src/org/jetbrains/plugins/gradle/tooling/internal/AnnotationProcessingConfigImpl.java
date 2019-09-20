// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AnnotationProcessingConfigImpl implements AnnotationProcessingConfig, Serializable {
  private final Set<String> myPaths;
  private final List<String> myArgs;
  private final String myProcessorOutput;
  private final boolean isTestSources;

  public AnnotationProcessingConfigImpl(Set<String> files, List<String> args, String output, boolean sources) {
    myProcessorOutput = output;
    isTestSources = sources;
    myPaths = files;
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
  @Override
  public boolean isTestSources() {
    return isTestSources;
  }

  @Nullable
  @Override
  public String getProcessorOutput() {
    return myProcessorOutput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AnnotationProcessingConfigImpl config = (AnnotationProcessingConfigImpl)o;
    return isTestSources == config.isTestSources &&
           Objects.equal(myProcessorOutput, config.myProcessorOutput) &&
           Objects.equal(myPaths, config.myPaths) &&
           Objects.equal(myArgs, config.myArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(myPaths, myArgs, myProcessorOutput, isTestSources);
  }
}
