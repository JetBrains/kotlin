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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PushDownProcessor<MemberInfo extends MemberInfoBase<Member>,
                               Member extends PsiElement,
                               Klass extends PsiElement> extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(PushDownProcessor.class);

  private NewSubClassData mySubClassData;
  private final PushDownDelegate<MemberInfo, Member> myDelegate;
  private final PushDownData<MemberInfo, Member> myPushDownData;

  public PushDownProcessor(@NotNull Klass sourceClass,
                           @NotNull List<MemberInfo> memberInfos,
                           @NotNull DocCommentPolicy javaDocPolicy) {
    super(sourceClass.getProject());
    myDelegate = PushDownDelegate.findDelegate(sourceClass);
    LOG.assertTrue(myDelegate != null);
    myPushDownData = new PushDownData<>(sourceClass, memberInfos, javaDocPolicy);
  }

  @Override
  @NotNull
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    return new PushDownUsageViewDescriptor<>((Klass)myPushDownData.getSourceClass(), myPushDownData.getMembersToMove());
  }

  @NotNull
  @Override
  protected Collection<? extends PsiElement> getElementsToWrite(@NotNull UsageViewDescriptor descriptor) {
    return Collections.singletonList(myPushDownData.getSourceClass());
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myPushDownData.getSourceClass());
    data.addElements(ContainerUtil.map(myPushDownData.getMembersToMove(), MemberInfoBase::getMember));
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(@NotNull UsageInfo[] usages) {
    final List<PsiElement> elements = new ArrayList<>();
    for (UsageInfo usage : usages) {
      elements.add(usage.getElement());
    }
    RefactoringEventData data = new RefactoringEventData();
    data.addElements(elements);
    return data;
  }

  @Override
  @NotNull
  protected UsageInfo[] findUsages() {
    final List<PsiElement> inheritors = myDelegate.findInheritors(myPushDownData);
    return ContainerUtil.map2Array(inheritors, UsageInfo.EMPTY_ARRAY, myDelegate::createUsageInfo);
  }

  @Override
  protected boolean preprocessUsages(@NotNull final Ref<UsageInfo[]> refUsages) {
    final MultiMap<PsiElement, String> conflicts = new MultiMap<>();
    myDelegate.checkSourceClassConflicts(myPushDownData, conflicts);
    final UsageInfo[] usagesIn = refUsages.get();
    if (usagesIn.length == 0) {
      mySubClassData = myDelegate.preprocessNoInheritorsFound(myPushDownData.getSourceClass(), getCommandName());
      if (mySubClassData == NewSubClassData.ABORT_REFACTORING) {
        return false;
      }
    }
    Runnable runnable = () -> ApplicationManager.getApplication().runReadAction(() -> {
      if (mySubClassData != null) {
        myDelegate.checkTargetClassConflicts(null, myPushDownData, conflicts, mySubClassData);
      }
      else {
        for (UsageInfo usage : usagesIn) {
          final PsiElement element = usage.getElement();
          if (element != null) {
            final PushDownDelegate delegate = PushDownDelegate.findDelegateForTarget(myPushDownData.getSourceClass(), element);
            if (delegate != null) {
              delegate.checkTargetClassConflicts(element, myPushDownData, conflicts, null);
            }
            else {
              conflicts.putValue(element, "Not supported source/target pair detected");
            }
          }
        }
      }
    });

    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, RefactoringBundle.message("detecting.possible.conflicts"), true, myProject)) {
      return false;
    }

    return showConflicts(conflicts, usagesIn);
  }

  @Override
  protected void refreshElements(@NotNull PsiElement[] elements) {
    if(elements.length == 1) {
      myPushDownData.setSourceClass(elements[0]);
    }
    else {
      LOG.assertTrue(false);
    }
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    try {
      pushDownToClasses(usages);
      myDelegate.removeFromSourceClass(myPushDownData);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  public void pushDownToClasses(@NotNull UsageInfo[] usages) {
    myDelegate.prepareToPush(myPushDownData);
    final PsiElement sourceClass = myPushDownData.getSourceClass();
    if (mySubClassData != null) {
      final PsiElement subClass = myDelegate.createSubClass(sourceClass, mySubClassData);
      if (subClass != null) {
        myDelegate.pushDownToClass(subClass, myPushDownData);
      }
    }
    else {
      for (UsageInfo usage : usages) {
        final PsiElement element = usage.getElement();
        if (element != null) {
          final PushDownDelegate targetDelegate = PushDownDelegate.findDelegateForTarget(sourceClass, element);
          if (targetDelegate != null) {
            targetDelegate.pushDownToClass(element, myPushDownData);
          }
        }
      }
    }
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return RefactoringBundle.message("push.members.down.title");
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.push.down";
  }
}
