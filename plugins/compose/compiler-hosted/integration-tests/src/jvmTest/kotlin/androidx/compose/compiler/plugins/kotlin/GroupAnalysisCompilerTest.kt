/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.mapping.ClassInfo
import androidx.compose.compiler.mapping.ComposeMappingErrorReporter
import androidx.compose.compiler.mapping.render
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Rule
import kotlin.test.Test

class GroupAnalysisCompilerTest(
    useFir: Boolean
) : AbstractCompilerTest(useFir) {

    override fun CompilerConfiguration.updateConfiguration() {
        // TODO test without function meta and optimize skipping groups flags
    }

    @Test
    fun topLevelFunction() {
        groups(
            """
            @Composable
            fun Test() {}
            """
        )
    }

    @Test
    fun lambdaInField() {
        groups(
            """
            class Test {
                val lambda = @Composable {
                    println("hello")
                }
            }
            """
        )
    }

    @Test
    fun loops() {
        groups(
            """
            @Composable
            fun Content(index: Int) {}

            @Composable
            fun Test() {
                for (i in 0..10) {
                    Content(i)
                }

                Content(1)
            }
            """
        )
    }

    @Test
    fun contentOf() {
        groups(
            """
            private fun contentOf(
                loading: @Composable ((State.Loading) -> Unit)?,
                success: @Composable ((State.Success) -> Unit)?,
                error: @Composable ((State.Error) -> Unit)?,
            ): @Composable () -> Unit {
                return if (loading != null || success != null || error != null) {
                    {
                        var draw = true
                        when (val state = getState()) {
                            is State.Loading -> if (loading != null) loading(state).also { draw = false }
                            is State.Success -> if (success != null) success(state).also { draw = false }
                            is State.Error -> if (error != null) error(state).also { draw = false }
                            is State.Empty -> {} // Skipped if rendering on the main thread.
                        }
                        if (draw) SubcomposeAsyncImageContent()
                    }
                } else {
                    { SubcomposeAsyncImageContent() }
                }
            }
            """,
            extra = """
             sealed class State {
                object Empty : State()
                class Loading() : State()
                class Success() : State()
                class Error() : State()
            }
            fun getState(): State = TODO()
            @Composable fun SubcomposeAsyncImageContent() {}
            """,
        )
    }

    @Test
    fun earlyReturn() = groups(
        """
            @Composable
            fun Test(text: String) {
                if (text.isEmpty()) return
                Text(text)
            }
        """,
        """
            @Composable fun Text(text: String) {}
        """,
    )

    @Test
    fun endToMarker() = groups(
        """
            @Composable
            fun Test(text: String) {
                Wrapper {
                    if (text.isEmpty()) return@Test
                    Text(text)
                }
            }
        """,
        """
            @Composable inline fun Wrapper(content: @Composable () -> Unit) = content()
            @Composable fun Text(text: String) {}
        """,
    )

    @Test
    fun endToMarkerNested() = groups(
        """
            @Composable
            fun Test(text: String) {
                Wrapper {
                    if (text.isEmpty()) return@Test
                    Text(text)

                    SecondWrapper {
                        if (text.isEmpty()) return@Wrapper
                        Text(text)
                    }
                }
            }
        """,
        """
            @Composable inline fun Wrapper(content: @Composable () -> Unit) = content()
            @Composable inline fun SecondWrapper(content: @Composable () -> Unit) = content()
            @Composable fun Text(text: String) {}
        """,
    )

    @Test
    fun earlyReturnCurrent() = groups(
        """
            @Composable fun Test(text: String) {
                if (Local.current) return
                if (text.isEmpty()) return

                Text(text)
            }
        """,
        """
            val Local = compositionLocalOf { false }
            @Composable fun Text(text: String) {}
        """,
    )

    @Test
    fun replaceableGroup() = groups(
        """
            @Composable fun <T> Test(text: T): T {
                Text(text.toString())
                return text
            }
        """,
        """
            @Composable fun Text(text: String) {}
        """,
    )

    @Test
    fun lambda() = groups(
        """
            val a = @Composable {}
        """
    )

    @JvmField
    @Rule
    val goldenTransformRule = GoldenTransformRule()

    private fun groups(
        @Language("kotlin") source: String,
        @Language("kotlin") extra: String = "",
        dumpIr: Boolean = false
    ) {
        val sources = listOf(
            SourceFile("Test.kt", wrapWithImports(source)),
            SourceFile("Extra.kt", wrapWithImports(extra))
        )
        if (dumpIr) {
            println(compileToIr(sources).dumpSrc())
        }

        val infos = createClassLoader(sources)
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
            .mapNotNull { file ->
                with(ComposeMappingErrorReporter.Default) {
                    ClassInfo(file.asByteArray()).takeIf { it.fileName == "Test.kt" }
                }
            }


        goldenTransformRule.verifyGolden(
            GoldenTransformTestInfo(
                source.trimIndent().trim(),
                infos.joinToString(separator = "\n") { it.render() }.trimIndent()
            )
        )
    }

    private fun wrapWithImports(source: String) =
        """
            import androidx.compose.runtime.*

            $source
        """.trimIndent()
}