package linkableContent

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL
import java.nio.file.Paths

class LinkableContentTest : BaseAbstractTest() {

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
                Assertions.assertEquals(2, it.documentation.size)
                Assertions.assertEquals(2, it.packages.size)
                Assertions.assertEquals(1, it.packages.first().documentation.size)
                Assertions.assertEquals(1, it.packages.last().documentation.size)
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
                Assertions.assertEquals(1, moduleChildren.size)
                val packageChildren = moduleChildren.first().children
                Assertions.assertEquals(2, packageChildren.size)
                packageChildren.forEach {
                    val name = it.name.substringBefore("Class")
                    val crl = it.safeAs<ClasslikePageNode>()?.content?.safeAs<ContentGroup>()?.children?.last()
                        ?.safeAs<ContentGroup>()?.children?.last()?.safeAs<ContentGroup>()?.children?.lastOrNull()
                        ?.safeAs<ContentTable>()?.children?.singleOrNull()
                        ?.safeAs<ContentGroup>()?.children?.singleOrNull().safeAs<ContentResolvedLink>()
                    Assertions.assertEquals(
                        "https://github.com/user/repo/tree/master/src/${name.toLowerCase()}Main/kotlin/${name}Class.kt#L3",
                        crl?.address
                    )
                }
            }
        }
    }

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
            renderingStage = { rootPageNode, dokkaContext ->
                val newRoot = DefaultSamplesTransformer(dokkaContext).invoke(rootPageNode)

                val moduleChildren = newRoot.children
                Assertions.assertEquals(1, moduleChildren.size)
                val packageChildren = moduleChildren.first().children
                Assertions.assertEquals(2, packageChildren.size)
                packageChildren.forEach { pageNode ->
                    val name = pageNode.name.substringBefore("Class")
                    val classChildren = pageNode.children
                    Assertions.assertEquals(2, classChildren.size)
                    val function = classChildren.find { it.name == "printWithExclamation" }
                    val text = function.cast<MemberPageNode>().content.cast<ContentGroup>().children.last()
                        .cast<ContentDivergentGroup>().children.single()
                        .cast<ContentDivergentInstance>().after
                        .cast<ContentGroup>().children.last()
                        .cast<ContentGroup>().children.last()
                        .cast<ContentGroup>().children.single()
                        .cast<ContentCodeBlock>().children.single().cast<ContentText>().text
                    Assertions.assertEquals(
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
                    .children.single { it.name == "Sample" }.cast<ClasslikePageNode>()
                val foo = sample
                    .children.single { it.name == "SampleInner" }.cast<ClasslikePageNode>()
                    .children.single { it.name == "foo" }.cast<MemberPageNode>()

                val returnTypeNode = foo.content.dfs {
                    val link = it.safeAs<ContentDRILink>()?.children
                    val child = link?.first().safeAs<ContentText>()
                    child?.text == "S"
                }?.safeAs<ContentDRILink>()

                Assertions.assertEquals(
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
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "js"
                    includes = listOf(Paths.get("$includesDir/include2.md").toString())
                }
                sourceSet {
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "jvm"
                    includes = listOf(Paths.get("$includesDir/include1.md").toString())
                }
            }
        }

        testFromData(configuration) {
            documentablesMergingStage = {
                Assertions.assertNotEquals(null, it.packages.first().documentation.values.single().dfs {
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
                module.documentation.entries.single {
                    it.key.displayName == "jvm"
                }.value.run {
                    Assertions.assertNotNull(dfs {
                        (it as? Text)?.body == "This is second JVM documentation for module example"
                    })

                    Assertions.assertNotNull(dfs {
                        (it as? Text)?.body == "This is JVM documentation for module example"
                    })
                }
            }
        }

    }
}
