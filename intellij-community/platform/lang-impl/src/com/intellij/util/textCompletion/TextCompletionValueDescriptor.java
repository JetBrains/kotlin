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
package com.intellij.util.textCompletion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Completion value descriptor for {@link ValuesCompletionProvider}.
 * Determines how to compare completion elements and how to present them.
 * <p>
 * Use {@link DefaultTextCompletionValueDescriptor} as default implementation.
 *
 * @param <T> completion element type.
 */
public interface TextCompletionValueDescriptor<T> extends Comparator<T> {
  @NotNull
  LookupElementBuilder createLookupBuilder(@NotNull T item);
}
