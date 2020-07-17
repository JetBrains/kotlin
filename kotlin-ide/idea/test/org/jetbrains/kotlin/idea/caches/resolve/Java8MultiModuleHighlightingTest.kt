/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.IDEA_TEST_DATA_DIR
import org.junit.internal.runners.JUnit38ClassRunner
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class Java8MultiModuleHighlightingTest : AbstractMultiModuleHighlightingTest() {
    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("multiModuleHighlighting")

    fun testDifferentJdk() {
        val module1 = module("jdk8", TestJdkKind.FULL_JDK)
        val module2 = module("mockJdk")

        module1.addDependency(module2)

        checkHighlightingInProject()
    }
}
