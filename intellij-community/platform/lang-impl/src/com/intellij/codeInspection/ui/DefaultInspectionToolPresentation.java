// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultInspectionToolPresentation extends DefaultInspectionToolResultExporter implements InspectionToolPresentation {
  protected static final Logger LOG = Logger.getInstance(DefaultInspectionToolPresentation.class);

  private DescriptorComposer myComposer;
  private volatile boolean isDisposed;

  @NotNull protected GlobalInspectionContextImpl myContext;

  public DefaultInspectionToolPresentation(@NotNull InspectionToolWrapper toolWrapper, @NotNull GlobalInspectionContextImpl context) {
    super(toolWrapper, context);
    myContext = context;
  }

  @Override
  public boolean isSuppressed(RefEntity element) {
    return mySuppressedElements.containsKey(element);
  }

  @Override
  public boolean isSuppressed(CommonProblemDescriptor descriptor) {
    return mySuppressedElements.containsValue(descriptor);
  }

  @Override
  public CommonProblemDescriptor @NotNull [] getSuppressedProblems(@NotNull RefEntity entity) {
    return mySuppressedElements.getOrDefault(entity, CommonProblemDescriptor.EMPTY_ARRAY);
  }


  @NotNull
  @Override
  public GlobalInspectionContextImpl getContext() {
    return myContext;
  }


  protected boolean isDisposed() {
    return isDisposed;
  }


  @Override
  public void cleanup() {
    isDisposed = true;
  }


  @NotNull
  @Override
  public HTMLComposerImpl getComposer() {
    if (myComposer == null) {
      myComposer = new DescriptorComposer(this);
    }
    return myComposer;
  }

  @Override
  public QuickFixAction @NotNull [] getQuickFixes(RefEntity @NotNull ... refElements) {
    return QuickFixAction.EMPTY;
  }

  @Override
  @Nullable
  public QuickFix findQuickFixes(@NotNull final CommonProblemDescriptor problemDescriptor,
                                 RefEntity entity,
                                 final String hint) {
    InspectionProfileEntry tool = getToolWrapper().getTool();
    return !(tool instanceof GlobalInspectionTool) ? null : ((GlobalInspectionTool)tool).getQuickFix(hint);
  }

  @Override
  public CommonProblemDescriptor @Nullable [] getDescriptions(@NotNull RefEntity refEntity) {
    final CommonProblemDescriptor[] problems = getProblemElements().getOrDefault(refEntity, null);
    if (problems == null) return null;

    if (!refEntity.isValid()) {
      ignoreElement(refEntity);
      return null;
    }

    return problems;
  }


  @Override
  public void ignoreElement(@NotNull final RefEntity refEntity) {
    myProblemElements.remove(refEntity);
  }

  @Override
  public void addProblemElement(@Nullable RefEntity refElement,
                                boolean filterSuppressed,
                                @NotNull CommonProblemDescriptor... descriptors) {
    super.addProblemElement(refElement, filterSuppressed, descriptors);

    final GlobalInspectionContextImpl context = getContext();
    if (context.isViewClosed() || !(refElement instanceof RefElement)) {
      return;
    }
    if (myToolWrapper instanceof LocalInspectionToolWrapper &&
        (!ApplicationManager.getApplication().isUnitTestMode() || GlobalInspectionContextImpl.TESTING_VIEW)) {
      context.initializeViewIfNeeded().doWhenDone(() -> context.getView().addProblemDescriptors(myToolWrapper, refElement, descriptors));
    }
  }

  @Override
  protected boolean filterResolvedItems() {
    return getContext().getUIOptions().FILTER_RESOLVED_ITEMS;
  }
}
