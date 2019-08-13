// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker

import com.completion.ranker.model.java.MLWhiteBox

class JavaCompletionRanker : RankerBase("java_features") {
    override fun rank(features: DoubleArray): Double {
        return MLWhiteBox.makePredict(features)
    }
}
