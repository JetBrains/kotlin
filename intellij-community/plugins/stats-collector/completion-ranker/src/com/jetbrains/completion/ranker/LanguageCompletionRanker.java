// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker;

import com.jetbrains.completion.feature.ModelMetadataEx;

public interface LanguageCompletionRanker {
  ModelMetadataEx getModelMetadata();

  double rank(double[] features);
}
