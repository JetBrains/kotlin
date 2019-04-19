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
package com.intellij.refactoring.introduceParameterObject;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.lang.LanguageExtension;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import com.intellij.refactoring.util.FixableUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @param <M> method type parameter for which delegate would work
 * @param <P> parameter info type, should correspond to the change signature's parameter info of the corresponding language
 * @param <C> IntroduceParameterObjectClassDescriptor's type which describes class of a new parameter to introduce:
 *           parameters to merge, their order, names and types as well as new class location, etc
 */
public abstract class IntroduceParameterObjectDelegate<M extends PsiNamedElement, P extends ParameterInfo, C extends IntroduceParameterObjectClassDescriptor<M, P>> {


  public static final LanguageExtension<IntroduceParameterObjectDelegate> EP_NAME =
    new LanguageExtension<>("com.intellij.refactoring.introduceParameterObject");

  /**
   * Find delegate by element language. Delegate must be registered as language extension.
   */
  public static
  <M extends PsiNamedElement,
    P extends ParameterInfo,
    C extends IntroduceParameterObjectClassDescriptor<M, P>>
  IntroduceParameterObjectDelegate<M, P, C> findDelegate(@NotNull PsiElement element) {
    return EP_NAME.forLanguage(element.getLanguage());
  }

  /**
   * Used from {@link com.intellij.refactoring.actions.IntroduceParameterObjectAction}
   * in order to detect if current element is inside method which is possible to refactor
   * @param element current element from DataContext
   */
  public abstract boolean isEnabledOn(PsiElement element);

  /**
   * Refactoring handler should choose which method to refactor based on element selected, e.g. suggest to choose super method if selected method overrides another method.
   * {@link AbstractIntroduceParameterObjectDialog} should be implemented to start the refactoring
   */
  @Nullable
  public abstract RefactoringActionHandler getHandler(PsiElement element);


  /**
   * @return {@link com.intellij.refactoring.changeSignature.MethodDescriptor#getParameters()}
   */
  public abstract List<P> getAllMethodParameters(M sourceMethod);

  /**
   * Resulted parameter info should implement {@link ParameterInfo#getActualValue(PsiElement, Object)} so the call site would be updated with actual values.
   * At the same time, usages in another languages should be correctly proceed. In order to do that, usage's delegate should be found and
   * {@link IntroduceParameterObjectDelegate#createNewParameterInitializerAtCallSite(PsiElement, IntroduceParameterObjectClassDescriptor, List, Object)}
   * should be called to provide actual value
   *
   * @return parameter info which would merge arguments on the call site, with name according to parameter class
   */
  public abstract P createMergedParameterInfo(C descriptor,
                                              M method,
                                              List<P> oldMethodParameters);

  /**
   * Call site should be updated according to the selected parameters, which correspond to the parameters to merge ({@link IntroduceParameterObjectClassDescriptor#getParamsToMerge()})
   * E.g. for the call foo(a, b, c) and parameter class which corresponds to the first 2 parameters, actual value should represent foo(new P(a, b), c)
   * @param substitutor should contain call substitutor before change signature replaced parameters
   */
  public abstract PsiElement createNewParameterInitializerAtCallSite(PsiElement callExpression,
                                                                     IntroduceParameterObjectClassDescriptor descriptor,
                                                                     List<? extends ParameterInfo> oldMethodParameters,
                                                                     Object substitutor);

  /**
   * Pass new parameter infos to the change info constructor which corresponds to the language of this delegate
   */
  public abstract ChangeInfo createChangeSignatureInfo(M method,
                                                       List<P> newParameterInfos,
                                                       boolean delegate);

  /**
   * Collect in usages reference to the parameter inside overridingMethod.
   * @param usages             collection to store usages
   * @param overridingMethod   method where usages would be searched.
   *                           As method could override method from another language,
   *                           overriding method defines the delegate to use though class descriptor could belong to another language.
   * @param classDescriptor    descriptor of the created class
   * @param parameterInfo      parameter which usages are collected.
   *                           parameterInfo#getOldIndex and overridingMethod should provide the real parameter to search
   * @param mergedParamName    name for new parameter, chosen in {@link #createMergedParameterInfo(IntroduceParameterObjectClassDescriptor, PsiNamedElement, List)}
   *
   * @param <M1>               method type of the original delegate
   * @param <P1>               parameter info type of the original delegate
   *
   * @return                   access level which is required for a parameter {@link ReadWriteAccessDetector.Access}. If write access is needed, both accessors are expected.
   */
  @Nullable
  public abstract <M1 extends PsiNamedElement, P1 extends ParameterInfo>
  ReadWriteAccessDetector.Access collectInternalUsages(Collection<FixableUsageInfo> usages,
                                                       M overridingMethod,
                                                       IntroduceParameterObjectClassDescriptor<M1, P1> classDescriptor,
                                                       P1 parameterInfo,
                                                       String mergedParamName);

  /**
   * Collect in {@code usages} fixes to generate field's accessors.
   * If {@link #collectInternalUsages(Collection, PsiNamedElement, IntroduceParameterObjectClassDescriptor, ParameterInfo, String)}
   * returns @NotNull value, corresponding to the parameter field requires an accessor.
   *
   * To detect what accessor is required, use {@code accessors[descriptor.getParamsToMerge()[paramIdx].getOldIdx()]}
   */
  public abstract void collectUsagesToGenerateMissedFieldAccessors(Collection<FixableUsageInfo> usages,
                                                                   M method,
                                                                   C descriptor,
                                                                   ReadWriteAccessDetector.Access[] accessors);

  /**
   * Collect in {@code usages} necessary fixes to change visibility, javadocs, etc
   */
  public abstract void collectAdditionalFixes(Collection<FixableUsageInfo> usages,
                                              M method,
                                              C descriptor);

  /**
   * Collect conflicts in {@code conflicts}
   */
  public abstract void collectConflicts(MultiMap<PsiElement, String> conflicts, UsageInfo[] infos, M method, C classDescriptor);
}
