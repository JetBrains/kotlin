/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.jetbrains.completion.feature.impl


object FeatureUtils {
    const val UNDEFINED: String = "UNDEFINED"
    const val INVALID_CACHE: String = "INVALID_CACHE"

    const val OTHER: String = "OTHER"
    const val NONE: String = "NONE"

    const val ML_RANK: String = "ml_rank"
    const val BEFORE_ORDER: String = "before_rerank_order"

    const val DEFAULT: String = "default"
    const val USE_UNDEFINED: String = "use_undefined"


    fun getOtherCategoryFeatureName(name: String): String = "$name=$OTHER"
    fun getUndefinedFeatureName(name: String): String = "$name=$UNDEFINED"
}
