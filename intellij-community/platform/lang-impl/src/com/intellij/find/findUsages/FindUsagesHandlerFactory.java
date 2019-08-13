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

package com.intellij.find.findUsages;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public abstract class FindUsagesHandlerFactory {
  public static final ExtensionPointName<FindUsagesHandlerFactory> EP_NAME = ExtensionPointName.create("com.intellij.findUsagesHandlerFactory");

  public abstract boolean canFindUsages(@NotNull PsiElement element);

  @Nullable
  public abstract FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, final boolean forHighlightUsages);
  
  public enum OperationMode {
    HIGHLIGHT_USAGES,
    USAGES_WITH_DEFAULT_OPTIONS,
    DEFAULT
  }
  
  public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, @NotNull OperationMode operationMode) {
    return createFindUsagesHandler(element, operationMode == OperationMode.HIGHLIGHT_USAGES);
  }
}
