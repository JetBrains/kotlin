/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.refactoring.changeSignature;

import com.intellij.psi.PsiElement;

import java.util.List;

public interface MethodDescriptor<P extends ParameterInfo, V> {

  enum ReadWriteOption { ReadWrite, Read, None }

  String getName();

  List<P> getParameters();

  int getParametersCount();

  V getVisibility();

  PsiElement getMethod();

  boolean canChangeVisibility();

  boolean canChangeParameters();

  boolean canChangeName();

  ReadWriteOption canChangeReturnType();
}
