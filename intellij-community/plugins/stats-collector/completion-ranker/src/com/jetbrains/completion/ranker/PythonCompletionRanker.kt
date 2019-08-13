// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker

import com.completion.ranker.model.python.MLWhiteBox

class PythonCompletionRanker : RankerBase("python_features") {
  override fun rank(features: DoubleArray?): Double {
    return MLWhiteBox.makePredict(features)
  }
}
