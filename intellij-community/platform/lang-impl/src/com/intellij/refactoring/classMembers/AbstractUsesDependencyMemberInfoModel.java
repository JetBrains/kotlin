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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dennis.Ushakov
 */
public abstract class AbstractUsesDependencyMemberInfoModel<T extends NavigatablePsiElement, C extends PsiElement, M extends MemberInfoBase<T>> extends DependencyMemberInfoModel<T, M> {
  protected final C myClass;

  public AbstractUsesDependencyMemberInfoModel(C aClass, @Nullable C superClass, boolean recursive) {
    super(new UsesMemberDependencyGraph<>(aClass, superClass, recursive), ERROR);
    myClass = aClass;
    setTooltipProvider(new MemberInfoTooltipManager.TooltipProvider<T, M>() {
      @Override
      public String getTooltip(M memberInfo) {
        return ((UsesMemberDependencyGraph<T, C, M>) myMemberDependencyGraph).getElementTooltip(memberInfo.getMember());
      }
    });
  }

  @Override
  public int checkForProblems(@NotNull M memberInfo) {
    final int problem = super.checkForProblems(memberInfo);
    return doCheck(memberInfo, problem);
  }

  protected abstract int doCheck(@NotNull M memberInfo, int problem);

  public void setSuperClass(C superClass) {
    setMemberDependencyGraph(new UsesMemberDependencyGraph<>(myClass, superClass, false));
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
