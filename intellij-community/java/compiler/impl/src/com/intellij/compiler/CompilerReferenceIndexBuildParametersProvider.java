// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.backwardRefs.JavaBackwardReferenceIndexWriter;

import java.util.Collections;
import java.util.List;

public class CompilerReferenceIndexBuildParametersProvider extends BuildProcessParametersProvider {
  @NotNull
  @Override
  public List<String> getVMArguments() {
    return CompilerReferenceService.isEnabled()
           ? Collections.singletonList("-D" + JavaBackwardReferenceIndexWriter.PROP_KEY + "=true")
           : Collections.emptyList();
  }
}
