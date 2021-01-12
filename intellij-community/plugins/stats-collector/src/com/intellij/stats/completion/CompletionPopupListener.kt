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

interface CompletionPopupListener {
    fun beforeDownPressed()
    fun downPressed()
    fun beforeUpPressed()
    fun upPressed()
    fun beforeBackspacePressed()
    fun afterBackspacePressed()
    fun beforeCharTyped(c: Char)

    class Adapter: CompletionPopupListener {
        override fun beforeDownPressed(): Unit = Unit
        override fun downPressed(): Unit = Unit
        override fun beforeUpPressed(): Unit = Unit
        override fun upPressed(): Unit = Unit
        override fun afterBackspacePressed(): Unit = Unit
        override fun beforeBackspacePressed(): Unit = Unit
        override fun beforeCharTyped(c: Char): Unit = Unit
    }
}