/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim.Mossienko
 */
public class CustomizingReferenceProvider extends PsiReferenceProvider implements CustomizableReferenceProvider {
  private final CustomizableReferenceProvider myProvider;
  @Nullable private Map<CustomizationKey, Object> myOptions;

  public CustomizingReferenceProvider(@NotNull CustomizableReferenceProvider provider) {
    myProvider = provider;
  }
  
  public <Option> void addCustomization(CustomizableReferenceProvider.CustomizationKey<Option> key, Option value) {
    if (myOptions == null) {
      myOptions = new HashMap<>(5);
    }
    myOptions.put(key,value);
  }
  
  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
    myProvider.setOptions(myOptions);
    final PsiReference[] referencesByElement = myProvider.getReferencesByElement(element, context);
    myProvider.setOptions(null);
    return referencesByElement;
  }

  @Override
  public void setOptions(@Nullable Map<CustomizationKey, Object> options) {
    myOptions = options;  // merge ?
  }

  @Override
  @Nullable
  public Map<CustomizationKey, Object> getOptions() {
    return myOptions;
  }
}
