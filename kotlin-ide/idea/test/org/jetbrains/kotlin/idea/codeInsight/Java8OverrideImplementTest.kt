/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.IDEA_TEST_DATA_DIR
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class Java8OverrideImplementTest : AbstractOverrideImplementTest() {
    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("codeInsight/overrideImplement/jdk8")
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    fun testOverrideCollectionStream() = doOverrideFileTest("stream")
}
