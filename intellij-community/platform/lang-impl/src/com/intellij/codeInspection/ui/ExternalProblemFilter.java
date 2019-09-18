// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class ExternalProblemFilter {
  private BiFunction<? super RefEntity, CommonProblemDescriptor[], Boolean> myFilter;

  public void registerProblemFilter(@NotNull BiFunction<? super RefEntity, CommonProblemDescriptor[], Boolean> filter) {
    myFilter = filter;
  }

  public boolean isFiltered(@NotNull RefEntity refElement, @NotNull CommonProblemDescriptor[] descriptors) {
    return myFilter.apply(refElement, descriptors);
  }

  public static ExternalProblemFilter getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ExternalProblemFilter.class);
  }
}
