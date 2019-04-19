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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * IMPORTANT: DO NOT USE IT
 * Supposed to be used ONLY by plugin allowing to sort completion using ml-ranking algorithm.
 * Needed to sort items from different sorters together.
 */
public abstract class CompletionFinalSorter {


  @NotNull
  public abstract Iterable<LookupElement> sort(@NotNull Iterable<LookupElement> initial, @NotNull CompletionParameters parameters);

  /**
   * For debugging purposes, provide weights by which completion will be sorted  
   */
  @NotNull
  public abstract Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(@NotNull Iterable<LookupElement> elements);
  
  
  @Deprecated
  public interface Factory {
    @NotNull
    CompletionFinalSorter newSorter();
  }

  @NotNull
  @SuppressWarnings("deprecation")
  public static CompletionFinalSorter newSorter() {
    Factory factory = ServiceManager.getService(Factory.class);
    return factory != null ? factory.newSorter() : EMPTY_SORTER;
  }


  private static final CompletionFinalSorter EMPTY_SORTER = new CompletionFinalSorter() {
    @NotNull
    @Override
    public Iterable<LookupElement> sort(@NotNull Iterable<LookupElement> initial, @NotNull CompletionParameters parameters) {
      return initial;
    }

    @NotNull
    @Override
    public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(@NotNull Iterable<LookupElement> elements) {
      return Collections.emptyMap();
    }
  };
  
  
}


