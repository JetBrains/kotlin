// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.memberPushDown;

import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PushDownUsageViewDescriptor<MemberInfo extends MemberInfoBase<Member>,
                                        Member extends PsiElement,
                                        Klass extends PsiElement> implements UsageViewDescriptor {
  private final PsiElement[] myMembers;
  private final String myProcessedElementsHeader;

  public PushDownUsageViewDescriptor(Klass aClass) {
    this(aClass, null);
  }

  public PushDownUsageViewDescriptor(Klass aClass, List<? extends MemberInfo> memberInfos) {
    myMembers = memberInfos != null ? ContainerUtil.map2Array(memberInfos, PsiElement.class, MemberInfoBase::getMember) : new PsiElement[]{aClass};
    myProcessedElementsHeader = RefactoringBundle.message("push.down.members.elements.header",
                                                          memberInfos != null ? DescriptiveNameUtil.getDescriptiveName(aClass) : "");
  }

  @Override
  public String getProcessedElementsHeader() {
    return myProcessedElementsHeader;
  }

  @Override
  @NotNull
  public PsiElement[] getElements() {
    return myMembers;
  }

  @NotNull
  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("classes.to.push.down.members.to", UsageViewBundle.getReferencesString(usagesCount, filesCount));
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return null;
  }

}
