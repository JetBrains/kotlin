/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ColoredTreeCellRenderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @deprecated to be removed in IDEA 2019.1
 */
@Deprecated
public abstract class MethodNodeBase<M extends PsiElement> extends MemberNodeBase<M> {
  protected MethodNodeBase(M method,
                           Set<M> called,
                           Project project,
                           Runnable cancelCallback) {
    super(method, called, project, cancelCallback);
  }

  @Override
  protected abstract MethodNodeBase<M> createNode(M caller, HashSet<M> called);

  public M getMethod() {
    return getMember();
  }
}
