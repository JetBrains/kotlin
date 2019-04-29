// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.openapi.components.ServiceManager
import com.jetbrains.completion.feature.FeatureManager
import com.jetbrains.completion.ranker.JavaCompletionRanker


interface Ranker {

    /**
     * Items are sorted by descending order, so item with the highest rank will be on top
     * @param relevance map from LookupArranger.getRelevanceObjects
     */
    fun rank(relevance: Map<String, Any>, userFactors: Map<String, Any?>): Double

    companion object {
        fun getInstance(): Ranker = ServiceManager.getService(Ranker::class.java)
    }
}

class MLRanker(manager: FeatureManager) : Ranker {
    private val featureTransformer = manager.createTransformer()
    private val ranker = JavaCompletionRanker()

    override fun rank(relevance: Map<String, Any>, userFactors: Map<String, Any?>): Double {
        val featureArray = featureTransformer.featureArray(relevance, userFactors)
        return ranker.rank(featureArray)
    }
}