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

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;

/**
 * @author dsl
 */
public class UsedByDependencyMemberInfoModel<T extends NavigatablePsiElement, C extends PsiElement, M extends MemberInfoBase<T>> extends DependencyMemberInfoModel<T, M> {

  public UsedByDependencyMemberInfoModel(C aClass) {
    super(new UsedByMemberDependencyGraph<>(aClass), ERROR);
    setTooltipProvider(new MemberInfoTooltipManager.TooltipProvider<T, M>() {
      @Override
      public String getTooltip(M memberInfo) {
        return ((UsedByMemberDependencyGraph<T, C, M>) myMemberDependencyGraph).getElementTooltip(memberInfo.getMember());
      }
    });
  }

  @Override
  public boolean isCheckedWhenDisabled(M member) {
    return false;
  }

  @Override
  public Boolean isFixedAbstract(M member) {
    return null;
  }
}
