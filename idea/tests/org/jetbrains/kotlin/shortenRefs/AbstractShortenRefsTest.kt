/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.shortenRefs

import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractShortenRefsTest : AbstractImportsTest() {
    override fun doTest(file: KtFile): String? {
        val selectionModel = myFixture.editor.selectionModel
        if (!selectionModel.hasSelection()) error("No selection in input file")
        ShortenReferences { ShortenReferences.Options(removeThis = true, removeThisLabels = true) }
                .process(file, selectionModel.selectionStart, selectionModel.selectionEnd)
        selectionModel.removeSelection()
        return null
    }

    override val nameCountToUseStarImportDefault: Int
        get() = Integer.MAX_VALUE
}
