// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.JdkFinder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JdkFinderImpl implements JdkFinder {
  @NotNull
  @Override
  public List<String> suggestHomePaths() {
    return JavaHomeFinder.suggestHomePaths();
  }
}
