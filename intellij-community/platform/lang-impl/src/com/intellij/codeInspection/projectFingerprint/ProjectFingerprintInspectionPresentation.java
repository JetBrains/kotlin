// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.projectFingerprint;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.DefaultInspectionToolPresentation;
import one.util.streamex.StreamEx;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ProjectFingerprintInspectionPresentation extends DefaultInspectionToolPresentation {

  public ProjectFingerprintInspectionPresentation(@NotNull InspectionToolWrapper toolWrapper,
                                                  @NotNull GlobalInspectionContextImpl context) {
    super(toolWrapper, context);
  }

  @Override
  protected void exportResults(@NotNull CommonProblemDescriptor[] descriptors,
                               @NotNull RefEntity refEntity,
                               @NotNull Consumer<? super Element> problemSink,
                               @NotNull Predicate<? super CommonProblemDescriptor> isDescriptorExcluded) {
    innerExportResults(problemSink, descriptors);
  }

  @Override
  public void exportResults(@NotNull Consumer<? super Element> resultConsumer,
                            @NotNull RefEntity refEntity,
                            @NotNull Predicate<? super CommonProblemDescriptor> isDescriptorExcluded) {
    if (isExcluded(refEntity)) return;
    CommonProblemDescriptor[] descriptors = getProblemElements().get(refEntity);
    if (descriptors == null) return;
    innerExportResults(resultConsumer, descriptors);
  }

  private void innerExportResults(@NotNull Consumer<? super Element> resultConsumer,
                                  CommonProblemDescriptor[] descriptors) {
    StreamEx.of(descriptors)
      .filter(descriptor -> !isExcluded(descriptor))
      .select(FileFingerprintDescriptor.class)
      .map(FileFingerprintDescriptor::getFileFingerprint)
      .forEach(f -> resultConsumer.accept(f.createElementContent()));
  }
}
