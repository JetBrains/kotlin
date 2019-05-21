/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSetInterner;
import com.intellij.util.containers.Interner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SuppressableInspectionTreeNode extends InspectionTreeNode {
  @NotNull
  private final InspectionToolPresentation myPresentation;
  private volatile Set<SuppressIntentionAction> myAvailableSuppressActions;
  private volatile String myPresentableName;
  private volatile Boolean myValid;
  private volatile NodeState myPreviousState;

  SuppressableInspectionTreeNode(@NotNull InspectionToolPresentation presentation, @NotNull InspectionTreeNode parent) {
    super(parent);
    myPresentation = presentation;
  }

  void nodeAdded() {
    dropProblemCountCaches();
    ReadAction.run(() -> myValid = calculateIsValid());
    //force calculation
    getProblemLevels();
  }

  @Override
  protected boolean doesNeedInternProblemLevels() {
    return true;
  }

  @NotNull
  public InspectionToolPresentation getPresentation() {
    return myPresentation;
  }

  public boolean canSuppress() {
    return getChildren().isEmpty();
  }

  public abstract boolean isAlreadySuppressedFromView();

  public abstract boolean isQuickFixAppliedFromView();

  @Override
  protected boolean isProblemCountCacheValid() {
    NodeState currentState = calculateState();
    if (!currentState.equals(myPreviousState)) {
      myPreviousState = currentState;
      return false;
    }
    return true;
  }

  @NotNull
  public synchronized Set<SuppressIntentionAction> getAvailableSuppressActions() {
    Set<SuppressIntentionAction> actions = myAvailableSuppressActions;
    if (actions == null) {
      actions = calculateAvailableSuppressActions();
      myAvailableSuppressActions = actions;
    }
    return actions;
  }

  public void removeSuppressActionFromAvailable(@NotNull SuppressIntentionAction action) {
    myAvailableSuppressActions.remove(action);
  }

  @Nullable
  public abstract RefEntity getElement();

  @Override
  public final synchronized boolean isValid() {
    Boolean valid = myValid;
    if (valid == null) {
      valid = calculateIsValid();
      myValid = valid;
    }
    return valid;
  }

  @Override
  public final synchronized String getPresentableText() {
    String name = myPresentableName;
    if (name == null) {
      name = calculatePresentableName();
      myPresentableName = name;
    }
    return name;
  }

  @Override
  void uiRequested() {
    nodeAdded();
    ReadAction.run(() -> {
      if (myPresentableName == null) {
        myPresentableName = calculatePresentableName();
        myValid = calculateIsValid();
        myAvailableSuppressActions = calculateAvailableSuppressActions();
      }
    });
  }

  @Nullable
  @Override
  public String getTailText() {
    if (isQuickFixAppliedFromView()) {
      return "";
    }
    if (isAlreadySuppressedFromView()) {
      return "Suppressed";
    }
    return !isValid() ? "No longer valid" : null;
  }

  @NotNull
  private Set<SuppressIntentionAction> calculateAvailableSuppressActions() {
    return getElement() == null
                                 ? Collections.emptySet()
                                 : calculateAvailableSuppressActions(myPresentation.getContext().getProject());
  }

  @NotNull
  public abstract Pair<PsiElement, CommonProblemDescriptor> getSuppressContent();

  @NotNull
  private Set<SuppressIntentionAction> calculateAvailableSuppressActions(@NotNull Project project) {
    if (myPresentation.isDummy()) return Collections.emptySet();
    final Pair<PsiElement, CommonProblemDescriptor> suppressContent = getSuppressContent();
    PsiElement element = suppressContent.getFirst();
    if (element == null) return Collections.emptySet();
    InspectionResultsView view = myPresentation.getContext().getView();
    if (view == null) return Collections.emptySet();
    InspectionViewSuppressActionHolder suppressActionHolder = view.getSuppressActionHolder();
    final SuppressIntentionAction[] actions = suppressActionHolder.getSuppressActions(myPresentation.getToolWrapper(), element);
    if (actions.length == 0) return Collections.emptySet();
    return suppressActionHolder.internSuppressActions(Arrays.stream(actions)
      .filter(action -> action.isAvailable(project, null, element))
      .collect(Collectors.toCollection(() -> ConcurrentCollectionFactory.createConcurrentSet(ContainerUtil.identityStrategy()))));
  }

  protected abstract String calculatePresentableName();

  protected abstract boolean calculateIsValid();

  protected void dropCache() {
    ReadAction.run(() -> doDropCache());
  }

  private void doDropCache() {
    myProblemLevels.drop();
    if (isQuickFixAppliedFromView() || isAlreadySuppressedFromView()) return;
    // calculate all data on background thread
    myValid = calculateIsValid();
    myPresentableName = calculatePresentableName();

    for (InspectionTreeNode child : getChildren()) {
      if (child instanceof SuppressableInspectionTreeNode) {
        ((SuppressableInspectionTreeNode)child).doDropCache();
      }
    }
  }

  private static class NodeState {
    private static final Interner<NodeState> INTERNER = new HashSetInterner<>();
    private final boolean isValid;
    private final boolean isSuppressed;
    private final boolean isFixApplied;
    private final boolean isExcluded;

    private NodeState(boolean isValid, boolean isSuppressed, boolean isFixApplied, boolean isExcluded) {
      this.isValid = isValid;
      this.isSuppressed = isSuppressed;
      this.isFixApplied = isFixApplied;
      this.isExcluded = isExcluded;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof NodeState)) return false;

      NodeState state = (NodeState)o;

      if (isValid != state.isValid) return false;
      if (isSuppressed != state.isSuppressed) return false;
      if (isFixApplied != state.isFixApplied) return false;
      if (isExcluded != state.isExcluded) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (isValid ? 1 : 0);
      result = 31 * result + (isSuppressed ? 1 : 0);
      result = 31 * result + (isFixApplied ? 1 : 0);
      result = 31 * result + (isExcluded ? 1 : 0);
      return result;
    }
  }

  private NodeState calculateState() {
    NodeState state = new NodeState(isValid(), isAlreadySuppressedFromView(), isQuickFixAppliedFromView(), isExcluded());
    synchronized (NodeState.INTERNER) {
      return NodeState.INTERNER.intern(state);
    }
  }
}
