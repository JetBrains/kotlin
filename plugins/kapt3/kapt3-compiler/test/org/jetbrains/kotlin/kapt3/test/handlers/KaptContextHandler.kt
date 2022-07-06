/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.kapt.base.test.JavaKaptContextTest
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.handlers.ClassFileToSourceKaptStubHandler.Companion.FILE_SEPARATOR
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.test.utils.withExtension

class KaptContextHandler(testServices: TestServices) : BaseKaptHandler(testServices) {
    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        val kaptContext = info.kaptContext
        val compilationUnits = convert(module, kaptContext, generateNonExistentClass = false)
        kaptContext.doAnnotationProcessing(
            emptyList(),
            listOf(JavaKaptContextTest.simpleProcessor()),
            additionalSources = compilationUnits
        )

        val stubJavaFiles = kaptContext.options.sourcesOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
        val actualRaw = stubJavaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' }).trimTrailingWhitespacesAndAddNewlineAtEOF()
        val expectedFile = module.files.first().originalFile.withExtension(".txt")
        assertions.assertEqualsToFile(expectedFile, actual)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
