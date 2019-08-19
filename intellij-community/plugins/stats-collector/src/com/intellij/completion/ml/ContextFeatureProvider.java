// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml;

import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public interface ContextFeatureProvider {
  LanguageExtension<ContextFeatureProvider> EP_NAME = new LanguageExtension<>("com.intellij.stats.completion.contextFeatures");

  @NotNull
  static List<ContextFeatureProvider> forLanguage(@NotNull Language language) {
    return EP_NAME.allForLanguageOrAny(language);
  }

  @NotNull
  String getName();

  /**
   * Will be invoked in EDT, should be fast enough
   */
  @NotNull
  Map<String, MLFeatureValue> calculateFeatures(LookupImpl lookup);
}
