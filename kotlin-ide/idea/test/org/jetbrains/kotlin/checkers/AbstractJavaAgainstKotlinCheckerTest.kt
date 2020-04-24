/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.idea.perf.forceUsingOldLightClassesForTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractJavaAgainstKotlinSourceCheckerTest : AbstractJavaAgainstKotlinCheckerTest() {
    fun doTest(path: String) {
        val relative = File(path).relativeTo(File(KotlinTestUtils.getHomeDirectory())).path
        doTest(true, true, relative.replace(".kt", ".java"), relative)
    }
}

abstract class AbstractJavaAgainstKotlinSourceCheckerWithoutUltraLightTest : AbstractJavaAgainstKotlinSourceCheckerTest() {
    override fun setUp() {
        super.setUp()
        forceUsingOldLightClassesForTest()
    }
}
