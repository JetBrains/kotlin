/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package linkableContent

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.*
import org.jsoup.Jsoup
import utils.TestOutputWriterPlugin
import utils.assertNotNull
import java.net.URL
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import utils.OnlyDescriptorsMPP

class LinkableContentTest : BaseAbstractTest() {

    @OnlyDescriptorsMPP("#3238")
    @Test
    fun `Include module and package documentation`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()
        val includesDir = getTestDataDir("linkable/includes").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
                }
                val jvmAndJsSecondCommonMain = sourceSet {
                    name = "jvmAndJsSecondCommonMain"
                    displayName = "jvmAndJsSecondCommonMain"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
                    includes = listOf(Paths.get("$includesDir/include2.md").toString())
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                    includes = listOf(Paths.get("$includesDir/include1.md").toString())
                }
            }
        }

        testFromData(configuration) {
            documentablesMergingStage = {
                assertEquals(2, it.documentation.size)
                assertEquals(2, it.packages.size)
                assertEquals(1, it.packages.first().documentation.size)
                assertEquals(1, it.packages.last().documentation.size)
            }
        }

    }

    @Test
    fun `Sources multiplatform class documentation`() {

        val testDataDir = getTestDataDir("linkable/sources").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"

            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
                }
                val jvmAndJsSecondCommonMain = sourceSet {
                    name = "jvmAndJsSecondCommonMain"
                    displayName = "jvmAndJsSecondCommonMain"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "$testDataDir/jsMain/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/jsMain/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "$testDataDir/jvmMain/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/jvmMain/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        testFromData(configuration) {
            renderingStage = { rootPageNode, dokkaContext ->
                val newRoot = SourceLinksTransformer(dokkaContext).invoke(rootPageNode)
                val moduleChildren = newRoot.children
                assertEquals(1, moduleChildren.size)
                val packageChildren = moduleChildren.first().children
                assertEquals(2, packageChildren.size)
                packageChildren.forEach {
                    val name = it.name.substringBefore("Class")
                    val signature = (it as? ClasslikePageNode)?.content?.dfs { it is ContentGroup && it.dci.kind == ContentKind.Symbol }.assertNotNull("signature")
                    val crl = signature.children.last().children[1] as? ContentResolvedLink
                    assertEquals(
                        "https://github.com/user/repo/tree/master/src/${name.toLowerCase()}Main/kotlin/${name}Class.kt#L7",
                        crl?.address
                    )
                }
            }
        }
    }

    @OnlyDescriptorsMPP("#3238")
    @Test
    fun `Samples multiplatform documentation`() {

        val testDataDir = getTestDataDir("linkable/samples").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
                }
                val jvmAndJsSecondCommonMain = sourceSet {
                    name = "jvmAndJsSecondCommonMain"
                    displayName = "jvmAndJsSecondCommonMain"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
                    samples = listOf("$testDataDir/jsMain/resources/Samples.kt")
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                    samples = listOf("$testDataDir/jvmMain/resources/Samples.kt")
                }
            }
        }

        testFromData(configuration) {
            renderingStage = { rootPageNode, _ ->
                // TODO [beresnev] :(((
//                val newRoot = DefaultSamplesTransformer(dokkaContext).invoke(rootPageNode)
                val newRoot = rootPageNode
                val moduleChildren = newRoot.children
                assertEquals(1, moduleChildren.size)
                val packageChildren = moduleChildren.first().children
                assertEquals(2, packageChildren.size)
                packageChildren.forEach { pageNode ->
                    val name = pageNode.name.substringBefore("Class")
                    val classChildren = pageNode.children
                    assertEquals(2, classChildren.size)
                    val function = classChildren.find { it.name == "printWithExclamation" }
                    val text = (function as MemberPageNode).content.let { it as ContentGroup }.children.last()
                        .let { it as ContentDivergentGroup }.children.single().after
                        .let { it as ContentGroup }.children.last()
                        .let { it as ContentGroup }.children.single()
                        .let { it as ContentCodeBlock }.children.single()
                        .let { it as ContentText }.text
                    assertEquals(
                        """|import p2.${name}Class
                                |fun main() { 
                                |   //sampleStart 
                                |   ${name}Class().printWithExclamation("Hi, $name") 
                                |   //sampleEnd
                                |}""".trimMargin(),
                        text
                    )
                }
            }
        }
    }

    @Test
    fun `Documenting return type for a function in inner class with generic parent`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class Sample<S>(first: S){
            |    inner class SampleInner {
            |       fun foo(): S = TODO()
            |    }
            |}
            |
        """.trimIndent(),
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        analysisPlatform = "jvm"
                        name = "js"
                    }
                }
            }
        ) {
            renderingStage = { module, _ ->
                val sample = module.children.single { it.name == "test" }
                    .children.single { it.name == "Sample" } as ClasslikePageNode
                val foo = sample
                    .children
                    .single { it.name == "SampleInner" }
                    .let { it as ClasslikePageNode }
                    .children
                    .single { it.name == "foo" }
                    .let { it as MemberPageNode }

                val returnTypeNode = foo.content.dfs {
                    val link = (it as? ContentDRILink)?.children
                    val child = link?.first() as? ContentText
                    child?.text == "S"
                } as? ContentDRILink

                assertEquals(
                    (sample.documentables.firstOrNull() as WithGenerics).generics.first().dri,
                    returnTypeNode?.address
                )
            }
        }
    }

    @Test
    fun `Include module and package documentation with codeblock`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()
        val includesDir = getTestDataDir("linkable/includes").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                sourceSet {
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "js"
                    includes = listOf(Paths.get("$includesDir/include2.md").toString())
                }
                sourceSet {
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "jvm"
                    includes = listOf(Paths.get("$includesDir/include1.md").toString())
                }
            }
        }

        testFromData(configuration) {
            documentablesMergingStage = {
                assertNotEquals(null, it.packages.first().documentation.values.single().dfs {
                    (it as? Text)?.body?.contains("@SqlTable") ?: false
                })
            }
        }

    }

    @Test
    fun `Include module with description parted in two files`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()
        val includesDir = getTestDataDir("linkable/includes").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
                }
                val jvmAndJsSecondCommonMain = sourceSet {
                    name = "jvmAndJsSecondCommonMain"
                    displayName = "jvmAndJsSecondCommonMain"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
                    includes = listOf(Paths.get("$includesDir/include2.md").toString())
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                    includes = listOf(
                        Paths.get("$includesDir/include1.md").toString(),
                        Paths.get("$includesDir/include11.md").toString()
                    )
                }
            }
        }

        testFromData(configuration) {
            documentablesMergingStage = { module ->
                val value = module.documentation.entries.single {
                    it.key.displayName == "jvm"
                }.value
                assertNotNull(value.dfs {
                    (it as? Text)?.body == "This is second JVM documentation for module example"
                })

                assertNotNull(value.dfs {
                    (it as? Text)?.body == "This is JVM documentation for module example"
                })
            }
        }
    }

    @Test
    fun `should have a correct link to declaration from another source set`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    sourceRoots = listOf("src/commonMain")
                    analysisPlatform = "common"
                    name = "common"
                    displayName = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/jvmMain/")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    displayName = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            /src/commonMain/main.kt
            class A
            /src/jvmMain/main.kt
            /**
            * link to [A]
            */
             class B
            """.trimIndent()
            ,
            pluginOverrides = listOf(writerPlugin),
            configuration = configuration
        ) {
            renderingStage = { _, _ ->
                val page =
                    Jsoup.parse(writerPlugin.writer.contents.getValue("root/[root]/-b/index.html"))
                val link = page.select(".paragraph a").single()
                assertEquals("../-a/index.html", link.attr("href"))
            }
        }
    }
}
