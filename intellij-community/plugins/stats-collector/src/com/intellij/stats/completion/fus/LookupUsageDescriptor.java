// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion.fus;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.lang.LanguageExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Allows to extend information in fus logs about shown lookup.
 * <p>
 * Each update in values we sent should be reflected in white-list scheme for `finished` event in `completion` group.
 * <p>
 * see {@link com.intellij.stats.completion.CompletionQualityTracker}
 */
@ApiStatus.Internal
public interface LookupUsageDescriptor {
  LanguageExtension<LookupUsageDescriptor> EP_NAME = new LanguageExtension<>("com.intellij.completion.stats.details");

  /*
   * The method is triggered after the lookup canceled. Use it to fill usageData with information to collect.
   */
  void customizeUsageData(@NotNull Lookup lookup, @NotNull FeatureUsageData usageData);
}
