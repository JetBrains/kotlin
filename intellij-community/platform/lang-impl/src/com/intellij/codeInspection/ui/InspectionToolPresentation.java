/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInspection.ui;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ProblemDescriptionsProcessor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.util.SynchronizedBidiMultiMap;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface InspectionToolPresentation extends ProblemDescriptionsProcessor {

  @NotNull
  InspectionToolWrapper getToolWrapper();

  default void patchToolNode(@NotNull InspectionTreeNode node,
                             @NotNull InspectionRVContentProvider provider,
                             boolean showStructure,
                             boolean groupBySeverity) {

  }

  @NotNull
  default RefElementNode createRefNode(@Nullable RefEntity entity,
                                       @NotNull InspectionTreeModel model,
                                       @NotNull InspectionTreeNode parent) {
    return new RefElementNode(entity, this, parent);
  }

  void updateContent();

  boolean hasReportedProblems();

  @NotNull
  Map<String, Set<RefEntity>> getContent();

  boolean isProblemResolved(@Nullable CommonProblemDescriptor descriptor);

  boolean isProblemResolved(@Nullable RefEntity entity);

  @NotNull
  Collection<RefEntity> getResolvedElements();

  @NotNull
  CommonProblemDescriptor[] getResolvedProblems(@NotNull RefEntity entity);

  void suppressProblem(@NotNull CommonProblemDescriptor descriptor);

  void suppressProblem(@NotNull RefEntity entity);

  boolean isSuppressed(RefEntity element);

  boolean isSuppressed(CommonProblemDescriptor descriptor);

  @NotNull
  CommonProblemDescriptor[] getSuppressedProblems(@NotNull RefEntity entity);

  void cleanup();
  @Nullable
  QuickFix findQuickFixes(@NotNull CommonProblemDescriptor descriptor,
                          RefEntity entity,
                          String hint);
  @NotNull
  HTMLComposerImpl getComposer();

  @NotNull
  QuickFixAction[] getQuickFixes(@NotNull RefEntity... refElements);
  @NotNull
  SynchronizedBidiMultiMap<RefEntity, CommonProblemDescriptor> getProblemElements();
  @NotNull
  Collection<CommonProblemDescriptor> getProblemDescriptors();
  void addProblemElement(@Nullable RefEntity refElement, boolean filterSuppressed, @NotNull CommonProblemDescriptor... descriptions);

  @NotNull
  GlobalInspectionContextImpl getContext();

  void exportResults(@NotNull Consumer<? super Element> resultConsumer,
                     @NotNull RefEntity refEntity,
                     @NotNull Predicate<? super CommonProblemDescriptor> isDescriptorExcluded);

  void exportResults(@NotNull Consumer<? super Element> resultConsumer,
                     @NotNull Predicate<? super RefEntity> isEntityExcluded,
                     @NotNull Predicate<? super CommonProblemDescriptor> isProblemExcluded);

  /** Override the preview panel for the entity. */
  @Nullable
  default JComponent getCustomPreviewPanel(@NotNull RefEntity entity) {
    return null;
  }

  /** Override the preview panel for the problem descriptor. */
  @Nullable
  default JComponent getCustomPreviewPanel(@NotNull CommonProblemDescriptor descriptor, @NotNull Disposable parent) {
    return null;
  }

  /** Additional actions applicable to the problem descriptor. May be (but not necessarily) related to the custom preview panel. */
  @Nullable
  default JComponent getCustomActionsPanel(@NotNull CommonProblemDescriptor descriptor, @NotNull Disposable parent) {
    return null;
  }

  /**
   * see {@link com.intellij.codeInspection.deadCode.DummyEntryPointsPresentation}
   * @return false only if contained problem elements contain real highlighted problem in code.
   */
  default boolean isDummy() {
    return false;
  }

  default boolean showProblemCount() {
    return true;
  }

  @Nullable
  HighlightSeverity getSeverity(@NotNull RefElement element);

  boolean isExcluded(@NotNull CommonProblemDescriptor descriptor);

  boolean isExcluded(@NotNull RefEntity entity);

  void amnesty(@NotNull RefEntity element);

  void exclude(@NotNull RefEntity element);

  void amnesty(@NotNull CommonProblemDescriptor descriptor);

  void exclude(@NotNull CommonProblemDescriptor descriptor);

  @NotNull
  static HighlightSeverity getSeverity(@Nullable RefEntity entity,
                                       @Nullable PsiElement psiElement,
                                       @NotNull InspectionToolPresentation presentation) {
    HighlightSeverity severity = null;
    final InspectionProfile profile = InspectionProjectProfileManager.getInstance(presentation.getContext().getProject()).getCurrentProfile();
    if (entity instanceof RefElement){
      final RefElement refElement = (RefElement)entity;
      severity = presentation.getSeverity(refElement);
    }
    if (severity == null) {
      severity = profile.getErrorLevel(HighlightDisplayKey.find(presentation.getToolWrapper().getShortName()), psiElement).getSeverity();
    }
    return severity;
  }
}
