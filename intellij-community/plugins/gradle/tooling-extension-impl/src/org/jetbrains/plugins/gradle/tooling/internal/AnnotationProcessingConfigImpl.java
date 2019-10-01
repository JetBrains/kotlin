// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AnnotationProcessingConfigImpl implements AnnotationProcessingConfig, Serializable {
  private final Set<String> myPaths;
  private final List<String> myArgs;

  public AnnotationProcessingConfigImpl(Set<String> files, List<String> args) {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AnnotationProcessingConfigImpl config = (AnnotationProcessingConfigImpl)o;
    return Objects.equal(myPaths, config.myPaths) &&
           Objects.equal(myArgs, config.myArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(myPaths, myArgs);
  }
}
