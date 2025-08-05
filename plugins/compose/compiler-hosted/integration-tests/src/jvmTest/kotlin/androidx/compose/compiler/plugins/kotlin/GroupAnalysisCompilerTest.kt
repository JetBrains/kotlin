/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.mapping.ClassInfo
import androidx.compose.compiler.mapping.ComposeMapping
import androidx.compose.compiler.mapping.ErrorReporter
import androidx.compose.compiler.mapping.render
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.runners.Parameterized
import kotlin.test.Test

class GroupAnalysisCompilerTest(
    useFir: Boolean,
    private val mapping: Boolean
) : AbstractCompilerTest(useFir) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useFir={0},mapping={1}")
        fun data() = arrayOf<Any>(
            arrayOf(true, true),
            arrayOf(true, false),
            arrayOf(false, false)
        )
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
            """,
            checkFunctionMeta = true
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
            checkFunctionMeta = true,
            checkOptimizeGroups = true
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
        checkOptimizeGroups = true
    )

    @Test
    fun lambda() = groups(
        """
            val a = @Composable {}
        """,
        checkFunctionMeta = true
    )

    @Test
    fun classMember() = groups(
        """
            interface Test {
                @Composable
                fun Test()
            }

            class TestImpl : Test {
                @Composable
                override fun Test() {}
            }
        """
    )

    @Test
    fun funInterface() = groups(
        """
            fun interface Test {
                @Composable
                fun Test()
            }

            fun Content() {
                Test {}
            }
        """
    )

    @JvmField
    @Rule
    val goldenTransformRule = GoldenTransformRule()

    private fun groups(
        @Language("kotlin") source: String,
        @Language("kotlin") extra: String = "",
        checkFunctionMeta: Boolean = false,
        checkOptimizeGroups: Boolean = false,
        dumpIr: Boolean = false
    ) {
        val sources = listOf(
            SourceFile("Test.kt", wrapWithImports(source)),
            SourceFile("Extra.kt", wrapWithImports(extra))
        )
        if (dumpIr) {
            println(compileToIr(sources).dumpSrc())
        }

        val matrix = parameterMatrix(checkFunctionMeta, checkOptimizeGroups)

        val classLoaders =
            matrix.map {
                val (meta, groups) = it
                val classLoader = createClassLoader(
                    sources,
                    additionalConfigurationParameters = {
                        it.put(ComposeConfiguration.GENERATE_FUNCTION_KEY_META_ANNOTATION_KEY, meta)
                        it.put(ComposeConfiguration.FEATURE_FLAGS, listOf(FeatureFlag.OptimizeNonSkippingGroups.name(groups)))
                    }
                )
                it to classLoader
            }

        val infos =
            classLoaders.map { (p, classLoader) ->
                val info = classLoader.allGeneratedFiles
                    .filter { it.relativePath.endsWith(".class") }
                    .mapNotNull { file ->
                        if (mapping) {
                            ComposeMapping.fromBytecode(ErrorReporter.Default, file.asByteArray())
                                .asProguardMapping(linePrefix = "")
                                .trim()
                                .takeIf { it.isNotBlank() }
                        } else {
                            with(ErrorReporter.Default) {
                                ClassInfo(file.asByteArray()).takeIf {
                                    it.fileName == "Test.kt" && it.methods.isNotEmpty()
                                }?.render()
                            }
                        }
                    }.joinToString(separator = "\n")
                p to info
            }


        goldenTransformRule.verifyGolden(
            GoldenTransformTestInfo(
                source.trimIndent().trim(),
                infos.joinToString(separator = "\n") { (p, info) ->
                    val (meta, groups) = p
                    val variant = "========= FunctionMeta: $meta, OptimizeGroups: $groups =========\n"
                    variant + info
                }.trimIndent()
            )
        )
    }

    private fun wrapWithImports(source: String) =
        """
            import androidx.compose.runtime.*

            $source
        """.trimIndent()

    private fun parameterMatrix(
        checkFunctionMeta: Boolean,
        checkOptimizeGroups: Boolean
    ): List<BooleanArray> = when {
        !checkOptimizeGroups && !checkFunctionMeta -> {
            listOf(
                booleanArrayOf(true, true)
            )
        }
        !checkOptimizeGroups -> {
            listOf(
                booleanArrayOf(true, true),
                booleanArrayOf(false, true)
            )
        }
        !checkFunctionMeta -> {
            listOf(
                booleanArrayOf(true, true),
                booleanArrayOf(true, false)
            )
        }
        else -> {
            listOf(
                booleanArrayOf(true, true),
                booleanArrayOf(false, true),
                booleanArrayOf(true, false),
                booleanArrayOf(false, false)
            )
        }
    }
}