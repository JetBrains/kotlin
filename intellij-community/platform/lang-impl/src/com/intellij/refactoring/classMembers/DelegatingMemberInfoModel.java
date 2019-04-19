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
import org.jetbrains.annotations.Nullable;

public class DelegatingMemberInfoModel<T extends PsiElement, M extends MemberInfoBase<T>> implements MemberInfoModel<T, M> {
  private MemberInfoModel<T, M> myDelegatingTarget;

  public DelegatingMemberInfoModel(MemberInfoModel<T, M> delegatingTarget) {
    myDelegatingTarget = delegatingTarget;
  }

  protected DelegatingMemberInfoModel() {

  }

  public MemberInfoModel getDelegatingTarget() {
    return myDelegatingTarget;
  }

  @Override
  public boolean isMemberEnabled(M member) {
    return myDelegatingTarget.isMemberEnabled(member);
  }

  @Override
  public boolean isCheckedWhenDisabled(M member) {
    return myDelegatingTarget.isCheckedWhenDisabled(member);
  }

  @Override
  public boolean isAbstractEnabled(M member) {
    return myDelegatingTarget.isAbstractEnabled(member);
  }

  @Override
  public boolean isAbstractWhenDisabled(M member) {
    return myDelegatingTarget.isAbstractWhenDisabled(member);
  }

  @Override
  public int checkForProblems(@NotNull M member) {
    return myDelegatingTarget.checkForProblems(member);
  }

  @Override
  public void memberInfoChanged(@NotNull MemberInfoChange<T, M> event) {
    myDelegatingTarget.memberInfoChanged(event);
  }

  @Override
  @Nullable
  public Boolean isFixedAbstract(M member) {
    return myDelegatingTarget.isFixedAbstract(member);
  }

  @Override
  public String getTooltipText(M member) {
    return myDelegatingTarget.getTooltipText(member);
  }
}
