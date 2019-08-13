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
package com.intellij.psi.util

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement

/**
 * Create a cached value with the given provider and non-tracked return value, store it in the PsiElement user data. If it's already stored, reuse it.
 * @return The cached value
 */
inline fun <T> PsiElement.getCachedValue(key: Key<CachedValue<T>>, provider: () -> CachedValueProvider<T>): T {
  return (getUserData(key) ?: return CachedValuesManager.getCachedValue(this, key, provider())).value
}