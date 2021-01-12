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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes parameter class to create or existing class, chosen to wrap parameters
 */
public abstract class IntroduceParameterObjectClassDescriptor<M extends PsiNamedElement, P extends ParameterInfo> {
  /**
   * Class name to create/existing class short name
   */
  @NotNull
  private final String myClassName;
  /**
   * Package name where class should be created/package name of the existing class. Won't be used if 'create inner class' option is chosen
   */
  private final String myPackageName;

  /**
   * Flag to search for existing class with fqn: {@code myPackageName.myClassName}
   */
  private final boolean myUseExistingClass;

  /**
   * Flag that inner class with name {@code myClassName} should be created in outer class: {@code method.getContainingClass()}
   */
  private final boolean myCreateInnerClass;

  /**
   * Visibility for newly created class
   */
  private final String myNewVisibility;

  /**
   * Flag to generate accessors for existing class when fields won't be accessible from new usages
   */
  private final boolean myGenerateAccessors;

  /**
   * Bundle of method parameters which should correspond to the newly created/existing class fields
   */
  private final P[] myParamsToMerge;

  /**
   * Store existing class found by fqn / created class in refactoring#performRefactoring
   */
  private PsiElement myExistingClass;
  /**
   * Detected compatible constructor of the existing class
   */
  private M myExistingClassCompatibleConstructor;

  public IntroduceParameterObjectClassDescriptor(@NotNull String className,
                                                 String packageName,
                                                 boolean useExistingClass,
                                                 boolean createInnerClass,
                                                 String newVisibility,
                                                 boolean generateAccessors,
                                                 P[] parameters) {
    myClassName = className;
    myPackageName = packageName;
    myUseExistingClass = useExistingClass;
    myCreateInnerClass = createInnerClass;
    myNewVisibility = newVisibility;
    myGenerateAccessors = generateAccessors;
    myParamsToMerge = parameters;
  }

  @NotNull
  public String getClassName() {
    return myClassName;
  }

  public String getPackageName() {
    return myPackageName;
  }

  public boolean isUseExistingClass() {
    return myUseExistingClass;
  }

  public boolean isCreateInnerClass() {
    return myCreateInnerClass;
  }

  public String getNewVisibility() {
    return myNewVisibility;
  }

  public P[] getParamsToMerge() {
    return myParamsToMerge;
  }

  public PsiElement getExistingClass() {
    return myExistingClass;
  }

  public void setExistingClass(PsiElement existingClass) {
    myExistingClass = existingClass;
  }

  public boolean isGenerateAccessors() {
    return myGenerateAccessors;
  }

  public P getParameterInfo(int oldIndex) {
    for (P info : myParamsToMerge) {
      if (info.getOldIndex() == oldIndex) {
        return info;
      }
    }
    return null;
  }

  /**
   * Corresponding field accessors how they should appear inside changed method body
   */
  public abstract String getSetterName(P paramInfo, @NotNull PsiElement context);
  public abstract String getGetterName(P paramInfo, @NotNull PsiElement context);

  /**
   * Called if use existing class is chosen only. Should find constructor to use
   */
  @Nullable
  public abstract M findCompatibleConstructorInExistingClass(M method);
  public M getExistingClassCompatibleConstructor() {
    return myExistingClassCompatibleConstructor;
  }
  public void setExistingClassCompatibleConstructor(M existingClassCompatibleConstructor) {
    myExistingClassCompatibleConstructor = existingClassCompatibleConstructor;
  }

  public abstract PsiElement createClass(M method, ReadWriteAccessDetector.Access[] accessors);
}
