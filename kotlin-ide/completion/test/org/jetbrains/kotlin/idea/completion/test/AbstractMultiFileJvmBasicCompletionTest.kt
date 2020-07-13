/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractMultiFileJvmBasicCompletionTest : KotlinCompletionTestCase() {
    protected fun doTest(testPath: String) {
        configureByFile(getTestName(false) + ".kt", "")
        val shouldFail = testPath.contains("NoSpecifiedType")
        AstAccessControl.testWithControlledAccessToAst(shouldFail, file.virtualFile, project, testRootDisposable, {
            testCompletion(file.text, JvmPlatforms.unspecifiedJvmPlatform, { completionType, invocationCount ->
                setType(completionType)
                complete(invocationCount)
                myItems
            }, CompletionType.BASIC, 0)
        })
    }

    override fun getTestDataDirectory() = File(KotlinTestUtils.getTestsRoot(this::class.java), getTestName(false))
}