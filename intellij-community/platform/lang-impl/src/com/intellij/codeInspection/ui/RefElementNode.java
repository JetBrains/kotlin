// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.reference.RefDirectory;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author max
 */
public class RefElementNode extends SuppressableInspectionTreeNode {
  private final Icon myIcon;
  @Nullable private final RefEntity myRefEntity;

  public RefElementNode(@Nullable RefEntity refEntity,
                        @NotNull InspectionToolPresentation presentation,
                        @NotNull InspectionTreeNode parent) {
    super(presentation, parent);
    myRefEntity = refEntity;
    myIcon = refEntity == null ? null : refEntity.getIcon(false);
  }

  @Override
  public final boolean isAlreadySuppressedFromView() {
    return getElement() != null && getPresentation().isSuppressed(getElement());
  }

  @Override
  @Nullable
  public RefEntity getElement() {
    return myRefEntity;
  }

  @Override
  @Nullable
  public Icon getIcon(boolean expanded) {
    return myIcon;
  }

  @Override
  protected String calculatePresentableName() {
    final RefEntity element = getElement();
    if (element == null) {
      return InspectionsBundle.message("inspection.reference.invalid");
    }
    return element.getRefManager().getRefinedElement(element).getName();
  }

  @Override
  protected boolean calculateIsValid() {
    final RefEntity refEntity = getElement();
    return refEntity != null && refEntity.isValid();
  }

  @Override
  public boolean isExcluded() {
    RefEntity element = getElement();
    if (isLeaf() && element != null) {
      return getPresentation().isExcluded(element);
    }
    return super.isExcluded();
  }

  @Override
  public void excludeElement() {
    RefEntity element = getElement();
    if (isLeaf() && element != null) {
      getPresentation().exclude(element);
      return;
    }
    super.excludeElement();
  }

  @Override
  public void amnestyElement() {
    RefEntity element = getElement();
    if (isLeaf() && element != null) {
      getPresentation().amnesty(element);
      return;
    }
    super.amnestyElement();
  }

  @Override
  public RefEntity getContainingFileLocalEntity() {
    final RefEntity element = getElement();
    return element instanceof RefElement && !(element instanceof RefDirectory)
           ? element
           : super.getContainingFileLocalEntity();
  }

  @Override
  protected void visitProblemSeverities(@NotNull TObjectIntHashMap<HighlightDisplayLevel> counter) {
    if (!isExcluded() && isLeaf() && !getPresentation().isProblemResolved(getElement()) && !getPresentation().isSuppressed(getElement())) {
      HighlightSeverity severity = InspectionToolPresentation.getSeverity(getElement(), null, getPresentation());
      HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
      if (!counter.adjustValue(level, 1)) {
        counter.put(level, 1);
      }
      return;
    }
    super.visitProblemSeverities(counter);
  }

  @Override
  public boolean isQuickFixAppliedFromView() {
    return isLeaf() && getPresentation().isProblemResolved(getElement());
  }

  @Nullable
  @Override
  public String getTailText() {
    if (getPresentation().isDummy()) {
      return "";
    }
    final String customizedText = super.getTailText();
    if (customizedText != null) {
      return customizedText;
    }
    return isLeaf() ? "" : null;
  }

  @NotNull
  @Override
  public Pair<PsiElement, CommonProblemDescriptor> getSuppressContent() {
    RefEntity refElement = getElement();
    PsiElement element = refElement instanceof RefElement ? ((RefElement)refElement).getPsiElement() : null;
    return Pair.create(element, null);
  }
}
