/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.kapt.cli.test

import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.kapt.cli.transformArgs
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import java.io.File

abstract class AbstractArgumentParsingTest {
    fun runTest(filePath: String) {
        val testFile = File(filePath)

        val sections = Section.parse(testFile)
        val before = sections.single { it.name == "before" }

        val messageCollector = MessageCollectorImpl()
        val transformedArgs = transformArgs(before.content.lines(), messageCollector, isTest = true)
        val actualAfter =
            if (messageCollector.hasErrors()) messageCollector.toString() else transformedArgs.joinToString(System.lineSeparator())
        val actual = sections.replacingSection("after", actualAfter).render()

        JUnit5Assertions.assertEqualsToFile(testFile, actual)
    }
}
