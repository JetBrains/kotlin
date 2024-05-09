/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.kapt.cli.test

import org.jetbrains.kotlin.kapt.cli.transformArgs
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.utils.TestMessageCollector
import java.io.File

private val LINE_SEPARATOR: String = System.getProperty("line.separator")

abstract class AbstractArgumentParsingTest {
    fun runTest(filePath: String) {
        val testFile = File(filePath)

        val sections = Section.parse(testFile)
        val before = sections.single { it.name == "before" }

        val messageCollector = TestMessageCollector()
        val transformedArgs = transformArgs(before.content.lines(), messageCollector, isTest = true)
        val actualAfter = if (messageCollector.hasErrors()) messageCollector.toString() else transformedArgs.joinToString(LINE_SEPARATOR)
        val actual = sections.replacingSection("after", actualAfter).render()

        JUnit5Assertions.assertEqualsToFile(testFile, actual)
    }
}
