/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest
import org.jetbrains.kotlin.psi.KtCodeFragment

abstract class AbstractCodeFragmentCompletionHandlerTest : AbstractCompletionHandlerTest(CompletionType.BASIC) {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath(testPath))
    }

    override fun doTest(testPath: String) {
        super.doTest(testPath)

        val fragment = myFixture.file as KtCodeFragment
        fragment.checkImports(testPath)
    }
}