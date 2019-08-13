/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.refactoring.classMembers;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class DependencyMemberInfoModel<T extends PsiElement, M extends MemberInfoBase<T>> implements MemberInfoModel<T, M> {
  protected MemberDependencyGraph<T, M> myMemberDependencyGraph;
  private final int myProblemLevel;
  private MemberInfoTooltipManager myTooltipManager;

  public DependencyMemberInfoModel(MemberDependencyGraph<T, M> memberDependencyGraph, int problemLevel) {
    myMemberDependencyGraph = memberDependencyGraph;
    myProblemLevel = problemLevel;
  }

  public void setTooltipProvider(MemberInfoTooltipManager.TooltipProvider <T, M> tooltipProvider) {
    myTooltipManager = new MemberInfoTooltipManager<>(tooltipProvider);
  }

  @Override
  public boolean isAbstractEnabled(M member) {
    return true;
  }

  @Override
  public boolean isAbstractWhenDisabled(M member) {
    return false;
  }

  @Override
  public boolean isMemberEnabled(M member) {
    return true;
  }

  @Override
  public int checkForProblems(@NotNull M memberInfo) {
    if (memberInfo.isChecked()) return OK;
    final T member = memberInfo.getMember();

    if (myMemberDependencyGraph.getDependent().contains(member)) {
      return myProblemLevel;
    }
    return OK;
  }

  public void setMemberDependencyGraph(MemberDependencyGraph<T, M> memberDependencyGraph) {
    myMemberDependencyGraph = memberDependencyGraph;
  }

  @Override
  public void memberInfoChanged(@NotNull MemberInfoChange<T, M> event) {
    memberInfoChanged(event.getChangedMembers());
  }

  public void memberInfoChanged(final Collection<? extends M> changedMembers) {
    if (myTooltipManager != null) myTooltipManager.invalidate();
    for (M changedMember : changedMembers) {
      myMemberDependencyGraph.memberChanged(changedMember);
    }
  }

  @Override
  public String getTooltipText(M member) {
    if (myTooltipManager != null) {
      return myTooltipManager.getTooltip(member);
    } else {
      return null;
    }
  }
}
