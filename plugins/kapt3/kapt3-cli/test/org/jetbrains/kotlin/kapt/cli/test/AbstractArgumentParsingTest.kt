/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.kapt.cli.test

import junit.framework.TestCase
import org.jetbrains.kotlin.daemon.TestMessageCollector
import org.jetbrains.kotlin.kapt.cli.transformArgs
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.runner.RunWith
import java.io.File

private val LINE_SEPARATOR: String = System.getProperty("line.separator")

@RunWith(JUnit3RunnerWithInners::class)
abstract class AbstractArgumentParsingTest : TestCase() {
    fun doTest(filePath: String) {
        val testFile = File(filePath)

        val sections = Section.parse(testFile)
        val before = sections.single { it.name == "before" }

        val messageCollector = TestMessageCollector()
        val transformedArgs = transformArgs(before.content.split(LINE_SEPARATOR), messageCollector, isTest = true)
        val actualAfter = if (messageCollector.hasErrors()) messageCollector.toString() else transformedArgs.joinToString(LINE_SEPARATOR)
        val actual = sections.replacingSection("after", actualAfter).render()

        KotlinTestUtils.assertEqualsToFile(testFile, actual)
    }
}