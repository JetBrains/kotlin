/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

interface BatchBaseConversion {
    fun runConversion(treeRoots: List<JKTreeElement>, context: NewJ2kConverterContext): Boolean
}

interface SequentialBaseConversion : BatchBaseConversion {
    fun runConversion(treeRoot: JKTreeElement, context: NewJ2kConverterContext): Boolean

    override fun runConversion(treeRoots: List<JKTreeElement>, context: NewJ2kConverterContext): Boolean {
        return treeRoots.asSequence().map { runConversion(it, context) }.max() ?: false
    }
}