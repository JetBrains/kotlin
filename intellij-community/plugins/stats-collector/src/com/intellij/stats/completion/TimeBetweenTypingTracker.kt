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

package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import com.intellij.openapi.project.Project
import com.intellij.stats.personalization.UserFactorDescriptions
import com.intellij.stats.personalization.UserFactorStorage
import java.util.concurrent.TimeUnit

/**
 * @author Vitaliy.Bibaev
 */
class TimeBetweenTypingTracker(private val project: Project) : PrefixChangeListener {
    private companion object {
        val MAX_ALLOWED_DELAY = TimeUnit.SECONDS.toMillis(10)
    }

    private var lastTypingTime: Long = -1L

    override fun beforeAppend(c: Char): Unit = prefixChanged()
    override fun beforeTruncate(): Unit = prefixChanged()

    private fun prefixChanged() {
        if (lastTypingTime == -1L) {
            lastTypingTime = System.currentTimeMillis()
            return
        }

        val currentTime = System.currentTimeMillis()
        val delay = currentTime - lastTypingTime
        if (delay > MAX_ALLOWED_DELAY) return
        UserFactorStorage.applyOnBoth(project, UserFactorDescriptions.TIME_BETWEEN_TYPING) { updater ->
            updater.fireTypingPerformed(delay.toInt())
        }

        lastTypingTime = currentTime
    }
}