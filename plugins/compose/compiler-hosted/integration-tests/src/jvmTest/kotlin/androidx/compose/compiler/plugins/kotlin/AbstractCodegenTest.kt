/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import java.io.File

var uniqueNumber = 0

abstract class AbstractCodegenTest(useFir: Boolean) : AbstractCompilerTest(useFir) {
    private fun dumpClasses(loader: GeneratedClassLoader) {
        for (
        file in loader.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }
        ) {
            println("------\nFILE: ${file.relativePath}\n------")
            println(file.asText())
        }
    }

    protected fun validateBytecode(
        @Language("kotlin")
        src: String,
        dumpClasses: Boolean = false,
        className: String = "Test_REPLACEME_${uniqueNumber++}",
        validate: (String) -> Unit,
    ) {
        validate(compileBytecode(src, dumpClasses, className))
    }

    protected fun compileToClassFiles(
        @Language("kotlin")
        src: String,
        className: String = "Test_REPLACEME_${uniqueNumber++}",
    ): List<OutputFile> {
        return classLoader(src, className)
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
    }

    protected fun compileBytecode(
        @Language("kotlin")
        src: String,
        dumpClasses: Boolean = false,
        className: String = "Test_REPLACEME_${uniqueNumber++}",
    ): String {
        val fileName = "$className.kt"

        val loader = classLoader(
            """
           @file:OptIn(
             InternalComposeApi::class,
           )
           package test

           import androidx.compose.runtime.*

           $src

            fun used(x: Any?) {}
        """,
            fileName, dumpClasses
        )

        return loader
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }.joinToString("\n") {
                it.asText().replace('$', '%').replace(className, "Test")
            }
    }

    protected fun classLoader(
        @Language("kotlin")
        source: String,
        fileName: String,
        dumpClasses: Boolean = false,
        additionalPaths: List<File> = emptyList(),
    ): GeneratedClassLoader {
        val loader = createClassLoader(listOf(SourceFile(fileName, source)), additionalPaths = additionalPaths)
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun classLoader(
        sources: Map<String, String>,
        dumpClasses: Boolean = false,
    ): GeneratedClassLoader {
        val loader = createClassLoader(
            sources.map { (fileName, source) -> SourceFile(fileName, source) }
        )
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun classLoader(
        platformSources: Map<String, String>,
        commonSources: Map<String, String>,
        dumpClasses: Boolean = false,
    ): GeneratedClassLoader {
        val loader = createClassLoader(
            platformSources.map { (fileName, source) -> SourceFile(fileName, source) },
            commonSources.map { (fileName, source) -> SourceFile(fileName, source) }
        )
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun classLoader(
        sources: Map<String, String>,
        additionalPaths: List<File>,
        dumpClasses: Boolean = false,
        forcedFirSetting: Boolean? = null,
    ): GeneratedClassLoader {
        val loader = createClassLoader(
            sources.map { (fileName, source) -> SourceFile(fileName, source) },
            additionalPaths = additionalPaths,
            forcedFirSetting = forcedFirSetting
        )
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun testCompile(@Language("kotlin") source: String, dumpClasses: Boolean = false, additionalPaths: List<File> = emptyList()) {
        classLoader(source, "Test.kt", dumpClasses, additionalPaths)
    }
}
