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

/**
 * @author Vitaliy.Bibaev
 */
object UserFactorDescriptions {
    private val IDS: MutableSet<String> = mutableSetOf()

    val COMPLETION_TYPE: UserFactorDescription<CompletionTypeUpdater, CompletionTypeReader> =
      Descriptor.register("completionType", ::CompletionTypeUpdater, ::CompletionTypeReader)
    val COMPLETION_FINISH_TYPE: UserFactorDescription<CompletionFinishTypeUpdater, CompletionFinishTypeReader> =
      Descriptor.register("completionFinishedType", ::CompletionFinishTypeUpdater, ::CompletionFinishTypeReader)
    val COMPLETION_USAGE: UserFactorDescription<CompletionUsageUpdater, CompletionUsageReader> =
      Descriptor.register("completionUsage", ::CompletionUsageUpdater, ::CompletionUsageReader)
    val PREFIX_LENGTH_ON_COMPLETION: UserFactorDescription<PrefixLengthUpdater, PrefixLengthReader> =
      Descriptor.register("prefixLength", ::PrefixLengthUpdater, ::PrefixLengthReader)
    val SELECTED_ITEM_POSITION: UserFactorDescription<ItemPositionUpdater, ItemPositionReader> =
      Descriptor.register("itemPosition", ::ItemPositionUpdater, ::ItemPositionReader)
    val TIME_BETWEEN_TYPING: UserFactorDescription<TimeBetweenTypingUpdater, TimeBetweenTypingReader> =
      Descriptor.register("timeBetweenTyping", ::TimeBetweenTypingUpdater, ::TimeBetweenTypingReader)
    val MNEMONICS_USAGE: UserFactorDescription<MnemonicsUsageUpdater, MnemonicsUsageReader> =
      Descriptor.register("mnemonicsUsage", ::MnemonicsUsageUpdater, ::MnemonicsUsageReader)

    fun isKnownFactor(id: String): Boolean = id in IDS

    private class Descriptor<out U : FactorUpdater, out R : FactorReader> private constructor(
      override val factorId: String,
      override val updaterFactory: (MutableDoubleFactor) -> U,
      override val readerFactory: (DailyAggregatedDoubleFactor) -> R) : UserFactorDescription<U, R> {
        companion object {
            fun <U : FactorUpdater, R : FactorReader> register(factorId: String,
                                                               updaterFactory: (MutableDoubleFactor) -> U,
                                                               readerFactory: (DailyAggregatedDoubleFactor) -> R): UserFactorDescription<U, R> {
                assert(!isKnownFactor(factorId)) { "Descriptor with id '$factorId' already exists" }
                IDS.add(factorId)
                return Descriptor(factorId, updaterFactory, readerFactory)
            }
        }
    }
}