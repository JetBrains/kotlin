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

import com.intellij.lang.LanguageExtension;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PushDownDelegate<MemberInfo extends MemberInfoBase<Member>,
                                      Member extends PsiElement> {
  public static final LanguageExtension<PushDownDelegate> EP_NAME = new LanguageExtension<>("com.intellij.refactoring.pushDown");

  @Nullable
  protected static <MemberInfo extends MemberInfoBase<Member>, Member extends PsiElement> PushDownDelegate<MemberInfo, Member> findDelegate(@NotNull PsiElement sourceClass) {
    return EP_NAME.forLanguage(sourceClass.getLanguage());
  }

  @Nullable
  protected static PushDownDelegate findDelegateForTarget(@NotNull PsiElement sourceClass, @NotNull PsiElement targetClass) {
    for (PushDownDelegate delegate : EP_NAME.allForLanguage(targetClass.getLanguage())) {
      if (delegate.isApplicableForSource(sourceClass)) {
        return delegate;
      }
    }
    return null;
  }

  /**
   * Check if delegate can process pushed members from the sourceClass. 
   * It is used to find appropriate delegate to process pushing from source class to target {@link #findDelegateForTarget(PsiElement, PsiElement)}.
   * 
   * Implementations are supposed to override this method when overriding default behaviour for the language, 
   * e.g. pushing members from groovy class to java, groovy could provide additional delegate which inherits delegate for java and accepts groovy sources.
   * Methods to process target class should be updated to cope with source of another language (e.g. calling super on PushDownData translated to java): 
   * {@link #checkTargetClassConflicts(PsiElement, PushDownData, MultiMap, NewSubClassData) },
   * {@link #pushDownToClass(PsiElement, PushDownData)}
   */
  protected abstract boolean isApplicableForSource(@NotNull PsiElement sourceClass);

  /**
   * Find classes to push members down.
   */
  protected abstract List<PsiElement> findInheritors(PushDownData<MemberInfo, Member> pushDownData);

  protected UsageInfo createUsageInfo(PsiElement element) {
    return new UsageInfo(element);
  }

  /**
   * Collect conflicts inside sourceClass assuming members would be removed,
   * e.g. check if members remaining in source class do not depend on moved members
   */
  protected abstract void checkSourceClassConflicts(PushDownData<MemberInfo, Member> pushDownData, MultiMap<PsiElement, String> conflicts);

  /**
   * Collect conflicts inside targetClass assuming methods would be pushed,
   * e.g. check if target class already has field with the same name, some references types
   * won't be accessible anymore, etc
   *
   * If {@code targetClass == null} (target class should be created), then subClassData would be not null
   */
  protected abstract void checkTargetClassConflicts(@Nullable PsiElement targetClass,
                                                    PushDownData<MemberInfo, Member> pushDownData,
                                                    MultiMap<PsiElement, String> conflicts,
                                                    @Nullable NewSubClassData subClassData);

  /**
   * Could be used e.g. to encode mutual references between moved members 
   */
  protected void prepareToPush(PushDownData<MemberInfo, Member> pushDownData) {}

  /**
   * Push members to the target class adjusting visibility, comments according to the policy, etc
   */
  protected abstract void pushDownToClass(PsiElement targetClass, PushDownData<MemberInfo, Member> pushDownData);

  /**
   * Remove members from the source class according to the abstract flag. 
   */
  protected abstract void removeFromSourceClass(PushDownData<MemberInfo, Member> pushDownData);

  /**
   * Called if no inheritors were found in {@link #findInheritors(PushDownData)}. Should warn that members would be deleted and 
   * suggest to create new target class if applicable
   * 
   * @return NewSubClassData.ABORT_REFACTORING if refactoring should be aborted
   *         null to proceed without inheritors (members would be deleted from the source class and not added to the target)
   *         new NewSubClassData(context, name) if new inheritor should be created with {@link #createSubClass(PsiElement, NewSubClassData)} 
   */
  protected NewSubClassData preprocessNoInheritorsFound(PsiElement sourceClass, String conflictDialogTitle) {
    final String message = RefactoringBundle.message("class.0.does.not.have.inheritors", DescriptiveNameUtil.getDescriptiveName(sourceClass)) + "\n" +
                           RefactoringBundle.message("push.down.will.delete.members");
    final int answer = Messages.showYesNoDialog(message, conflictDialogTitle, Messages.getWarningIcon());
    if (answer != Messages.YES) {
      return NewSubClassData.ABORT_REFACTORING;
    }
    return null;
  }

  /**
   * Create sub class with {@code subClassData.getNewClassName()} in the specified context if no inheritors were found
   */
  @Nullable
  protected PsiElement createSubClass(PsiElement aClass, NewSubClassData subClassData) {
    return null;
  }
}
