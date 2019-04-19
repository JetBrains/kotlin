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

package com.intellij.stats.personalization

import com.intellij.stats.personalization.impl.*
import com.jetbrains.completion.feature.BinaryFeature
import com.jetbrains.completion.feature.CategoricalFeature
import com.jetbrains.completion.feature.DoubleFeature

/**
 * @author Vitaliy.Bibaev
 */
object UserFactorDescriptions {
    val COMPLETION_TYPE: Descriptor<CompletionTypeUpdater, CompletionTypeReader> = Descriptor("completionType", ::CompletionTypeUpdater, ::CompletionTypeReader)
    val COMPLETION_FINISH_TYPE: Descriptor<CompletionFinishTypeUpdater, CompletionFinishTypeReader> =
            Descriptor("completionFinishedType", ::CompletionFinishTypeUpdater, ::CompletionFinishTypeReader)
    val COMPLETION_USAGE: Descriptor<CompletionUsageUpdater, CompletionUsageReader> = Descriptor("completionUsage", ::CompletionUsageUpdater, ::CompletionUsageReader)
    val PREFIX_LENGTH_ON_COMPLETION: Descriptor<PrefixLengthUpdater, PrefixLengthReader> = Descriptor("prefixLength", ::PrefixLengthUpdater, ::PrefixLengthReader)
    val SELECTED_ITEM_POSITION: Descriptor<ItemPositionUpdater, ItemPositionReader> = Descriptor("itemPosition", ::ItemPositionUpdater, ::ItemPositionReader)
    val TIME_BETWEEN_TYPING: Descriptor<TimeBetweenTypingUpdater, TimeBetweenTypingReader> = Descriptor("timeBetweenTyping", ::TimeBetweenTypingUpdater, ::TimeBetweenTypingReader)
    val MNEMONICS_USAGE: Descriptor<MnemonicsUsageUpdater, MnemonicsUsageReader> = Descriptor("mnemonicsUsage", ::MnemonicsUsageUpdater, ::MnemonicsUsageReader)

    fun binaryFeatureDescriptor(feature: BinaryFeature): Descriptor<BinaryFeatureUpdater, BinaryFeatureReader> {
        return Descriptor("binaryFeature:${feature.name}", ::BinaryFeatureUpdater, ::BinaryFeatureReader)
    }

    fun doubleFeatureDescriptor(feature: DoubleFeature): Descriptor<DoubleFeatureUpdater, DoubleFeatureReader> {
        return Descriptor("doubleFeature:${feature.name}", ::DoubleFeatureUpdater, ::DoubleFeatureReader)
    }

    fun categoricalFeatureDescriptor(feature: CategoricalFeature): Descriptor<CategoryFeatureUpdater, CategoryFeatureReader> {
        return Descriptor("categoricalFeature:${feature.name}",
                { CategoryFeatureUpdater(feature.categories, it) },
                ::CategoryFeatureReader)
    }

    class Descriptor<out U : FactorUpdater, out R : FactorReader>(
            override val factorId: String,
            override val updaterFactory: (MutableDoubleFactor) -> U,
            override val readerFactory: (DailyAggregatedDoubleFactor) -> R) : UserFactorDescription<U, R>
}