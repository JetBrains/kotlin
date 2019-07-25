// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch.completion.lookup;

import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.compiler.chainsSearch.ChainRelevance;
import org.jetbrains.annotations.NotNull;

public final class JavaRelevantChainLookupElement extends LookupElementDecorator<LookupElement> {
  private final ChainRelevance myChainRelevance;

  public JavaRelevantChainLookupElement(final @NotNull LookupElement delegate, final @NotNull ChainRelevance relevance) {
    super(delegate);
    myChainRelevance = relevance;
  }

  @NotNull
  public ChainRelevance getChainRelevance() {
    return myChainRelevance;
  }

  @Override
  public AutoCompletionPolicy getAutoCompletionPolicy() {
    return AutoCompletionPolicy.NEVER_AUTOCOMPLETE;
  }
}
