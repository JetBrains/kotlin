/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
