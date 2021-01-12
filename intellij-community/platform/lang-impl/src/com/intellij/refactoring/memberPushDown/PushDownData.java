/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.refactoring.memberPushDown;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.util.DocCommentPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PushDownData<MemberInfo extends MemberInfoBase<Member>,
                          Member extends PsiElement> {
  private PsiElement mySourceClass;
  private final List<MemberInfo> myMembersToMove;
  private final DocCommentPolicy myCommentPolicy;

  PushDownData(@NotNull PsiElement sourceClass,
               @NotNull List<MemberInfo> membersToMove,
               @NotNull DocCommentPolicy commentPolicy) {
    mySourceClass = sourceClass;
    myMembersToMove = membersToMove;
    myCommentPolicy = commentPolicy;
  }
  @NotNull
  public PsiElement getSourceClass() {
    return mySourceClass;
  }
  @NotNull
  public List<MemberInfo> getMembersToMove() {
    return myMembersToMove;
  }
  @NotNull
  public DocCommentPolicy getCommentPolicy() {
    return myCommentPolicy;
  }

  public void setSourceClass(@NotNull PsiElement sourceClass) {
    mySourceClass = sourceClass;
  }
}
