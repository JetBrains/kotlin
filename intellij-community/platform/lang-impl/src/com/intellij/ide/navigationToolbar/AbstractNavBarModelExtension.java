/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.navigationToolbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author gregsh
 */
public abstract class AbstractNavBarModelExtension implements NavBarModelExtension {
  @Nullable
  @Override
  public abstract String getPresentableText(Object object);

  @Nullable
  @Override
  public PsiElement adjustElement(PsiElement psiElement) {
    return psiElement;
  }

  @Nullable
  @Override
  public PsiElement getParent(PsiElement psiElement) {
    return null;
  }

  @NotNull
  @Override
  public Collection<VirtualFile> additionalRoots(Project project) {
    return Collections.emptyList();
  }

  public boolean processChildren(Object object, Object rootElement, Processor<Object> processor) {
    return true;
  }
}
