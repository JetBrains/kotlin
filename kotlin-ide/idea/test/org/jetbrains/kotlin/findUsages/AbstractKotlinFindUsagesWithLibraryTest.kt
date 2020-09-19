/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.doTestWithFIRFlagsByPath
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.runAll

abstract class AbstractKotlinFindUsagesWithLibraryFirTest : AbstractKotlinFindUsagesWithLibraryTest() {
    override fun isFirPlugin(): Boolean = true

    override fun <T : PsiElement> doTest(path: String) = doTestWithFIRFlagsByPath(path) {
        super.doTest<T>(path)
    }
}

abstract class AbstractKotlinFindUsagesWithLibraryTest : AbstractFindUsagesTest() {
    private val mockLibraryFacility = MockLibraryFacility(
        source = IDEA_TEST_DATA_DIR.resolve("findUsages/libraryUsages/_library")
    )

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { mockLibraryFacility.tearDown(module) },
            ThrowableRunnable { super.tearDown() }
        )
    }
}
