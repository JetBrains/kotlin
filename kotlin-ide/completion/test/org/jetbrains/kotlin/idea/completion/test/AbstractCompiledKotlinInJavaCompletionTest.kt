/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File

abstract class AbstractCompiledKotlinInJavaCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    private val mockLibraryFacility = MockLibraryFacility(
      source = File(COMPLETION_TEST_DATA_BASE, "injava/mockLib"),
      attachSources = false
    )

    override fun getPlatform() = JvmPlatforms.unspecifiedJvmPlatform

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

    override fun defaultCompletionType() = CompletionType.BASIC
}
