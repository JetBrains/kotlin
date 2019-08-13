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

package com.intellij.completion.tracker

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import com.intellij.stats.completion.StagePosition


class PositionTrackingListener(private val lookup: LookupImpl): PrefixChangeListener {
    private var stage = 0

    override fun beforeAppend(c: Char): Unit = update()
    override fun beforeTruncate(): Unit = update()

    private fun update() {
        lookup.items.forEachIndexed { index, lookupElement ->
            val position = StagePosition(stage, index)
            UserDataLookupElementPositionTracker.addElementPosition(lookup, lookupElement, position)
        }
        stage++
    }
}