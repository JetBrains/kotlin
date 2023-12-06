/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.nio.file.Files

/**
 * Provides ability to quickly write tests with 'inline source code' aka passing Kotlin source code as String.
 *
 * This interface can be injected into any test class constructor.
 *
 * ### Example
 * ```
 * class MyTest(
 *     private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis
 * ) {
 *     @Test
 *     fun `test - something important`() {
 *         val myFile = inlineSourceCodeAnalysis.createKtFile("class Foo")
 *         analyze(myFile) {
 *             // Use analysis session to write advanced tests
 *         }
 *     }
 * }
 * ```
 */
interface InlineSourceCodeAnalysis {
    fun createKtFile(@Language("kotlin") sourceCode: String): KtFile
}

/**
 * Extension used to inject an instance of [InlineSourceCodeAnalysis] into tests.
 */
class InlineSourceCodeAnalysisExtension : ParameterResolver, AfterEachCallback {
    private companion object {
        val namespace: Namespace = Namespace.create(Any())
        val tempDirKey = Any()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == InlineSourceCodeAnalysis::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val temporaryDirectory = Files.createTempDirectory("inlineSourceCode").toFile()
        extensionContext.getStore(namespace.append(extensionContext.requiredTestClass)).put(tempDirKey, temporaryDirectory)
        return InlineSourceCodeAnalysisImpl(temporaryDirectory)
    }

    override fun afterEach(context: ExtensionContext) {
        context.getStore(namespace.append(context.requiredTestClass))?.get(tempDirKey, File::class.java)?.deleteRecursively()
    }
}

/**
 * Simple implementation [InlineSourceCodeAnalysis]
 */
private class InlineSourceCodeAnalysisImpl(private val tempDir: File) : InlineSourceCodeAnalysis {
    override fun createKtFile(@Language("kotlin") sourceCode: String): KtFile {
        return createStandaloneAnalysisApiSession(tempDir, listOf(sourceCode))
            .modulesWithFiles.entries.single()
            .value.single() as KtFile
    }
}
