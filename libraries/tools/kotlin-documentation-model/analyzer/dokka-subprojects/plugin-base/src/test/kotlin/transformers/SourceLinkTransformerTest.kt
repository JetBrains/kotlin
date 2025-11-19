/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLinkTransformerTest : BaseAbstractTest() {

    private fun Element.getSourceLink() = select(".symbol .source-link")
        .select("a[href]")
        .attr("href")

    @Test
    fun `source link should lead to name`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |/**
            |* Marks the annotated declaration as deprecated. ...
            |*/
            |@Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER, TYPEALIAS)
            |@MustBeDocumented
            |public annotation class Deprecated(
            |    val message: String,
            |    val replaceWith: ReplaceWith = ReplaceWith(""),
            |    val level: DeprecationLevel = DeprecationLevel.WARNING
            |)
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/testpackage/-deprecated/index.html")
                val sourceLink = page.getSourceLink()

                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L8",
                    sourceLink
                )
            }
        }
    }

    @Test
    fun `source link should be for actual typealias`() {
        val mppConfiguration = dokkaConfiguration {
            moduleName = "test"
            sourceSets {
                sourceSet {
                    name = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                    classpath = listOf(commonStdlibPath!!)
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                }
                sourceSet {
                    name = "jvm"
                    dependentSourceSets = setOf(DokkaSourceSetID("test", "common"))
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                    classpath = listOf(commonStdlibPath!!)
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect class Foo
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |class Bar
                |actual typealias Foo = Bar
                |
            """.trimMargin(),
            mppConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("test/example/-foo/index.html")
                val sourceLink = page.getSourceLink()

                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/kotlin/jvm/Test.kt#L4",
                    sourceLink
                )
            }
        }
    }

    @Test
    fun `each declaration should include exactly one source link in the case #4338`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |val ff = 0 // #4338
            |fun ff() = 0
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val pageFf = writerPlugin.writer.renderedContent("root/testpackage/ff.html")
                fun Element.getAllSourceElements() = select(".symbol .source-link")
                assertEquals(
                    2,
                    pageFf.getAllSourceElements().size
                )
                assertEquals(
                    listOf("https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L4", "https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L3"),
                    pageFf.getAllSourceElements().map { it.select("a[href]").attr("href") }
                )
            }
        }
    }

    @Test
    fun `each declaration should include exactly one source link in the case #4049`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |fun overloadWithVararg(novararg: String){}  // #4049
            |fun overloadWithVararg(vararg elements: String){}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val pageOverloadWithVararg = writerPlugin.writer.renderedContent("root/testpackage/overload-with-vararg.html")
                fun Element.getAllSourceElements() = select(".symbol .source-link")

                assertEquals(
                    2,
                    pageOverloadWithVararg.getAllSourceElements().size
                )
                assertEquals(
                    listOf("https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L2", "https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L3"),
                    pageOverloadWithVararg.getAllSourceElements().map { it.select("a[href]").attr("href") }
                )
            }
        }
    }
}
