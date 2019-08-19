// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml;

import com.intellij.codeInsight.completion.CompletionLocation;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public interface ElementFeatureProvider {
  LanguageExtension<ElementFeatureProvider> EP_NAME = new LanguageExtension<>("com.intellij.stats.completion.elementFeatures");

  @NotNull
  static List<ElementFeatureProvider> forLanguage(Language language) {
    return EP_NAME.allForLanguageOrAny(language);
  }

  String getName();

  Map<String, MLFeatureValue> calculateFeatures(@NotNull LookupElement element,
                                                @NotNull CompletionLocation location,
                                                @NotNull ContextFeatures contextFeatures);
}
