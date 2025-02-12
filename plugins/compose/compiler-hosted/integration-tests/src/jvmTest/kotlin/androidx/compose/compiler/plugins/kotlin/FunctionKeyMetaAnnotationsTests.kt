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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.Rule
import org.junit.Test

/* ktlint-disable max-line-length */
class FunctionKeyMetaAnnotationsTests(useFir: Boolean) : AbstractCodegenTest(useFir) {

    @JvmField
    @Rule
    val goldenTransformRule = GoldenTransformRule()

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.GENERATE_FUNCTION_KEY_META_ANNOTATION_KEY, true)
    }

    @Test
    fun testSimpleComposable(): Unit = verifyGoldenBytecodeRender(
        """
            import androidx.compose.runtime.Composable  
            @Composable 
            fun Example() {}
        """.trimIndent()
    )

    @Test
    fun testComposableLambda(): Unit = verifyGoldenBytecodeRender(
        """
            import androidx.compose.runtime.Composable  
            @Composable 
            fun Foo(child: @Composable () -> Unit) { child() }
            
            @Composable 
            fun Bar() {
                Foo { 
                    print("A")
                }
            
                Foo {
                    print("B")
                }
            }
        """.trimIndent()
    )

    @Test
    fun testNonComposableLambda(): Unit = verifyGoldenBytecodeRender(
        """
            import androidx.compose.runtime.Composable
             
            fun higherOrderFunction(child: Any.() -> Unit) {
            
            }
            
            
            @Composable 
            fun Foo() {
                higherOrderFunction {
                    print("Foo")
                }
            } 
        """.trimIndent()
    )

    @Test
    fun testCapturingComposableLambda(): Unit = verifyGoldenBytecodeRender(
        """
         import androidx.compose.runtime.*
        
         @Composable
         fun Wrapper(child: @Composable () -> Unit) {
             child()
         }

         @Composable
         fun Foo() {
             var state by remember { mutableStateOf(0) }
             Wrapper {
                 println("%state")
             }
         }

        """.trimIndent().replace("%", "$")
    )

    @Test
    fun testCapturingComposableLambda_entryPoint(): Unit = verifyGoldenBytecodeRender(
        """
         import androidx.compose.runtime.*
         
         fun runApplication(child: @Composable () -> Unit) {
             /* Pretend to be an entry point */
         }
         
         fun Foo() {
             var state = 255
             runApplication {
                 println("%state")
             }
         }

        """.trimIndent().replace("%", "$")
    )


    private fun verifyGoldenBytecodeRender(@Language("kotlin") source: String) {
        val files = compileToClassFiles(source, "Test.kt")
        val rendered = renderAnnotatedDeclarations(files)
        goldenTransformRule.verifyGolden(GoldenTransformTestInfo(source, rendered))
    }

    private fun renderAnnotatedDeclarations(files: List<OutputFile>): String = buildString {
        files.forEachIndexed forEachFile@{ fileIndex, file ->
            val node = ClassNode()
            val reader = ClassReader(file.asByteArray())
            reader.accept(node, 0)


            appendLine("${node.name} {")

            node.methods.forEachIndexed forEachMethod@{ methodIndex, method ->
                val annotation = method.visibleAnnotations.orEmpty().find { annotationNode ->
                    annotationNode.desc == "Landroidx/compose/runtime/internal/FunctionKeyMeta;"
                }
                appendLine(
                    "    ${method.name} ${method.desc} ${
                        annotation?.values?.chunked(2)?.joinToString(", ", prefix = "[", postfix = "]") { (k, v) -> "$k=$v" }
                    }"
                )
            }
            appendLine("}")
            if (fileIndex != files.lastIndex) appendLine()
        }
    }.trim()
}
