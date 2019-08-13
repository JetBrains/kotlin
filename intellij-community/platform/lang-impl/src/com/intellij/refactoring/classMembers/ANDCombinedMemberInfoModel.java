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

public class ANDCombinedMemberInfoModel<T extends PsiElement, M extends MemberInfoBase<T>> implements MemberInfoModel<T, M> {
  private final MemberInfoModel<T, M> myModel1;
  private final MemberInfoModel<T, M> myModel2;
  private final MemberInfoTooltipManager<T, M> myTooltipManager =
    new MemberInfoTooltipManager<>(new MemberInfoTooltipManager.TooltipProvider<T, M>() {
      @Override
      public String getTooltip(M memberInfo) {
        final String tooltipText1 = myModel1.getTooltipText(memberInfo);
        if (tooltipText1 != null) return tooltipText1;
        return myModel2.getTooltipText(memberInfo);
      }
    });


  public ANDCombinedMemberInfoModel(MemberInfoModel<T, M> model1, MemberInfoModel<T, M> model2) {
    myModel1 = model1;
    myModel2 = model2;
  }

  @Override
  public boolean isMemberEnabled(M member) {
    return myModel1.isMemberEnabled(member) && myModel2.isMemberEnabled(member);
  }

  @Override
  public boolean isCheckedWhenDisabled(M member) {
    return myModel1.isCheckedWhenDisabled(member) && myModel2.isCheckedWhenDisabled(member);
  }

  @Override
  public boolean isAbstractEnabled(M member) {
    return myModel1.isAbstractEnabled(member) && myModel2.isAbstractEnabled(member);
  }

  @Override
  public boolean isAbstractWhenDisabled(M member) {
    return myModel1.isAbstractWhenDisabled(member) && myModel2.isAbstractWhenDisabled(member);
  }

  @Override
  public int checkForProblems(@NotNull M member) {
    return Math.max(myModel1.checkForProblems(member), myModel2.checkForProblems(member));
  }

  @Override
  public void memberInfoChanged(@NotNull MemberInfoChange<T, M> event) {
    myTooltipManager.invalidate();
    myModel1.memberInfoChanged(event);
    myModel2.memberInfoChanged(event);
  }

  @Override
  public Boolean isFixedAbstract(M member) {
    final Boolean fixedAbstract1 = myModel1.isFixedAbstract(member);
    if(fixedAbstract1 == null) return null;
    if(fixedAbstract1.equals(myModel2.isFixedAbstract(member))) return fixedAbstract1;
    return null;
  }

  public MemberInfoModel<T, M> getModel1() {
    return myModel1;
  }

  public MemberInfoModel<T, M> getModel2() {
    return myModel2;
  }

  @Override
  public String getTooltipText(M member) {
    return myTooltipManager.getTooltip(member);
  }
}
