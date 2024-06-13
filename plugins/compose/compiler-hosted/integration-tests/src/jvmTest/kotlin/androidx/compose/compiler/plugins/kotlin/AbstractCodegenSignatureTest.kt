/*
 * Copyright 2024 The Android Open Source Project
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

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.junit.Rule

abstract class AbstractCodegenSignatureTest(useFir: Boolean) : AbstractCodegenTest(useFir) {
    private fun OutputFile.printApi(): String {
        return printPublicApi(asText(), relativePath)
    }

    @JvmField
    @Rule
    val goldenTransformRule = GoldenTransformRule()

    protected fun checkApi(
        @Language("kotlin") src: String,
        dumpClasses: Boolean = false
    ) {
        val className = "Test_REPLACEME_${uniqueNumber++}"
        val fileName = "$className.kt"

        val loader = classLoader(
            """
           import androidx.compose.runtime.*

           $src
        """,
            fileName, dumpClasses
        )

        val apiString = loader
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
            .joinToString(separator = "\n") { it.printApi() }
            .replace(className, "Test")

        goldenTransformRule.verifyGolden(
            GoldenTransformTestInfo(
                src.trimIndent().trim(),
                apiString.trimIndent().trim()
            )
        )
    }

    protected fun codegen(
        @Language("kotlin") text: String,
        dumpClasses: Boolean = false
    ) {
        codegenNoImports(
            """
           import androidx.compose.runtime.*

           $text

            fun used(x: Any?) {}
        """,
            dumpClasses
        )
    }

    private fun codegenNoImports(
        @Language("kotlin") text: String,
        dumpClasses: Boolean = false
    ) {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }
}
