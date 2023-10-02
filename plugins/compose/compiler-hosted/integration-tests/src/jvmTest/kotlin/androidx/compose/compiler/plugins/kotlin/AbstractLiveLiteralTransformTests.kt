/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import androidx.compose.compiler.plugins.kotlin.lower.LiveLiteralTransformer
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.junit.Assert.assertEquals

abstract class AbstractLiveLiteralTransformTests(
    useFir: Boolean
) : AbstractIrTransformTest(useFir) {
    private fun computeKeys(files: List<SourceFile>): List<String> {
        var builtKeys = mutableSetOf<String>()
        compileToIr(
            files,
            registerExtensions = { configuration ->
                val liveLiteralsEnabled = configuration.getBoolean(
                    ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY
                )
                val liveLiteralsV2Enabled = configuration.getBoolean(
                    ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY
                )
                ComposePluginRegistrar.registerCommonExtensions(this)
                IrGenerationExtension.registerExtension(
                    this,
                    object : IrGenerationExtension {
                        override fun generate(
                            moduleFragment: IrModuleFragment,
                            pluginContext: IrPluginContext
                        ) {
                            val symbolRemapper = DeepCopySymbolRemapper()
                            val keyVisitor = DurableKeyVisitor(builtKeys)
                            val transformer = object : LiveLiteralTransformer(
                                liveLiteralsEnabled || liveLiteralsV2Enabled,
                                liveLiteralsV2Enabled,
                                keyVisitor,
                                pluginContext,
                                symbolRemapper,
                                ModuleMetricsImpl("temp")
                            ) {
                                override fun makeKeySet(): MutableSet<String> {
                                    return super.makeKeySet().also { builtKeys = it }
                                }
                            }
                            transformer.lower(moduleFragment)
                        }
                    }
                )
            }
        )
        return builtKeys.toList()
    }

    // since the lowering will throw an exception if duplicate keys are found, all we have to do
    // is run the lowering
    protected fun assertNoDuplicateKeys(@Language("kotlin") src: String) {
        computeKeys(listOf(SourceFile("Test.kt", src)))
    }

    // For a given src string, a
    protected fun assertKeys(vararg keys: String, makeSrc: () -> String) {
        val builtKeys = computeKeys(listOf(SourceFile("Test.kt", makeSrc())))
        assertEquals(
            keys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            },
            builtKeys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            }
        )
    }

    // test: have two src strings (before/after) and assert that the keys of the params didn't change
    protected fun assertDurableChange(before: String, after: String) {
        val beforeKeys = computeKeys(listOf(SourceFile("Test.kt", before)))
        val afterKeys = computeKeys(listOf(SourceFile("Test.kt", after)))

        assertEquals(
            beforeKeys.toList().sorted().joinToString(separator = "\n"),
            afterKeys.toList().sorted().joinToString(separator = "\n")
        )
    }

    protected fun assertTransform(
        unchecked: String,
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable
            $unchecked
        """.trimIndent(),
        dumpTree = dumpTree
    )
}
