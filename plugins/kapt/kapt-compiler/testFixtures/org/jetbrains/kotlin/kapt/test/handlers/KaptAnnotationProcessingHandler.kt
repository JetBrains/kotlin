/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.kapt.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt.base.test.JavaKaptContextUtils
import org.jetbrains.kotlin.kapt.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt.test.handlers.KaptStubConverterHandler.Companion.FILE_SEPARATOR
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF

class KaptAnnotationProcessingHandler(testServices: TestServices) : BaseKaptHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives)

    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        val kaptContext = info.kaptContext
        val compilationUnits = convert(module, kaptContext, generateNonExistentClass = false)
        kaptContext.doAnnotationProcessing(
            emptyList(),
            listOf(JavaKaptContextUtils.simpleProcessor()),
            additionalSources = compilationUnits
        )

        val stubJavaFiles = kaptContext.options.sourcesOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
        val actualRaw = stubJavaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' }).trimTrailingWhitespacesAndAddNewlineAtEOF()
        assertEqualsToDump(extension = ".txt", actual)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
