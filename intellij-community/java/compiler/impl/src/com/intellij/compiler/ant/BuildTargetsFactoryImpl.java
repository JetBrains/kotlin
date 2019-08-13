// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.compiler.ant;

import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.TestOnly;

public class BuildTargetsFactoryImpl extends BuildTargetsFactory {


  @Override
  public Generator createComment(final String comment) {
    return new Comment(comment);
  }

  @Override
  @TestOnly
  public GenerationOptions getDefaultOptions(Project project) {
    return new GenerationOptionsImpl(project, true, false, false, true, ArrayUtilRt.EMPTY_STRING_ARRAY);
  }
}