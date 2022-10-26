package enums

import matchers.content.*
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
import java.io.File
import java.net.URL

class KotlinEnumsTest : BaseAbstractTest() {

    @Test
    fun `should preserve enum source ordering for documentables`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   ZERO,
            |   ONE,
            |   TWO,
            |   THREE,
            |   FOUR,
            |   FIVE,
            |   SIX,
            |   SEVEN,
            |   EIGHT,
            |   NINE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                val testPackage = module.packages[0]
                assertEquals("testpackage", testPackage.name)

                val testEnum = testPackage.classlikes[0] as DEnum
                assertEquals("TestEnum", testEnum.name)

                val enumEntries = testEnum.entries
                assertEquals(10, enumEntries.count())

                assertEquals("ZERO", enumEntries[0].name)
                assertEquals("ONE", enumEntries[1].name)
                assertEquals("TWO", enumEntries[2].name)
                assertEquals("THREE", enumEntries[3].name)
                assertEquals("FOUR", enumEntries[4].name)
                assertEquals("FIVE", enumEntries[5].name)
                assertEquals("SIX", enumEntries[6].name)
                assertEquals("SEVEN", enumEntries[7].name)
                assertEquals("EIGHT", enumEntries[8].name)
                assertEquals("NINE", enumEntries[9].name)
            }
        }
    }

    @Test
    fun `should preserve enum source ordering for generated pages`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   ZERO,
            |   ONE,
            |   TWO,
            |   THREE,
            |   FOUR,
            |   FIVE,
            |   SIX,
            |   SEVEN,
            |   EIGHT,
            |   NINE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            pagesGenerationStage = { rootPage ->
                val packagePage = rootPage.children[0]
                assertEquals("testpackage", packagePage.name)

                val testEnumNode = packagePage.children[0]
                assertEquals("TestEnum", testEnumNode.name)

                val enumEntries = testEnumNode.children.filterIsInstance<ClasslikePage>()
                assertEquals(10, enumEntries.size)

                assertEquals("ZERO", enumEntries[0].name)
                assertEquals("ONE", enumEntries[1].name)
                assertEquals("TWO", enumEntries[2].name)
                assertEquals("THREE", enumEntries[3].name)
                assertEquals("FOUR", enumEntries[4].name)
                assertEquals("FIVE", enumEntries[5].name)
                assertEquals("SIX", enumEntries[6].name)
                assertEquals("SEVEN", enumEntries[7].name)
                assertEquals("EIGHT", enumEntries[8].name)
                assertEquals("NINE", enumEntries[9].name)
            }
        }
    }

    @Test
    fun `should preserve enum source ordering for rendered entries`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   ZERO,
            |   ONE,
            |   TWO,
            |   THREE,
            |   FOUR,
            |   FIVE,
            |   SIX,
            |   SEVEN,
            |   EIGHT,
            |   NINE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val enumEntriesOnPage = writerPlugin.writer.renderedContent("root/testpackage/-test-enum/index.html")
                    .select("div.table[data-togglable=Entries]")
                    .select("div.table-row")
                    .select("div.keyValue")
                    .select("div.title")
                    .select("a")

                val enumEntries = enumEntriesOnPage.map { it.text() }
                assertEquals(10, enumEntries.size)

                assertEquals("ZERO", enumEntries[0])
                assertEquals("ONE", enumEntries[1])
                assertEquals("TWO", enumEntries[2])
                assertEquals("THREE", enumEntries[3])
                assertEquals("FOUR", enumEntries[4])
                assertEquals("FIVE", enumEntries[5])
                assertEquals("SIX", enumEntries[6])
                assertEquals("SEVEN", enumEntries[7])
                assertEquals("EIGHT", enumEntries[8])
                assertEquals("NINE", enumEntries[9])
            }
        }
    }

    @Test
    fun `should preserve enum source ordering for navigation menu`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   ZERO,
            |   ONE,
            |   TWO,
            |   THREE,
            |   FOUR,
            |   FIVE,
            |   SIX,
            |   SEVEN,
            |   EIGHT,
            |   NINE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val sideMenu = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")

                assertEquals("ZERO", sideMenu.select("#root-nav-submenu-0-0-0").text())
                assertEquals("ONE", sideMenu.select("#root-nav-submenu-0-0-1").text())
                assertEquals("TWO", sideMenu.select("#root-nav-submenu-0-0-2").text())
                assertEquals("THREE", sideMenu.select("#root-nav-submenu-0-0-3").text())
                assertEquals("FOUR", sideMenu.select("#root-nav-submenu-0-0-4").text())
                assertEquals("FIVE", sideMenu.select("#root-nav-submenu-0-0-5").text())
                assertEquals("SIX", sideMenu.select("#root-nav-submenu-0-0-6").text())
                assertEquals("SEVEN", sideMenu.select("#root-nav-submenu-0-0-7").text())
                assertEquals("EIGHT", sideMenu.select("#root-nav-submenu-0-0-8").text())
                assertEquals("NINE", sideMenu.select("#root-nav-submenu-0-0-9").text())
            }
        }
    }

    fun TestOutputWriter.navigationHtml(): Element = contents.getValue("navigation.html").let { Jsoup.parse(it) }

    @Test
    fun `should handle companion object within enum`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2;
            |   companion object {}
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    assertTrue(p.isNotEmpty(), "Package list cannot be empty")
                    p.first().classlikes.let { c ->
                        assertTrue(c.isNotEmpty(), "Classlikes list cannot be empty")

                        val enum = c.first() as DEnum
                        assertNotNull(enum.companion)
                    }
                }
            }
        }
    }

    @Test
    fun enumWithMethods() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |
            |interface Sample {
            |    fun toBeImplemented(): String
            |}
            |
            |enum class TestEnum: Sample {
            |    E1 {
            |        override fun toBeImplemented(): String = "e1"
            |    }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    p.first().classlikes.let { c ->
                        val enum = c.first { it is DEnum } as DEnum
                        val first = enum.entries.first()

                        assertNotNull(first.functions.find { it.name == "toBeImplemented" })
                    }
                }
            }
        }
    }

    @Test
    fun enumWithAnnotationsOnEntries() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |enum class TestEnum {
            |    /**
            |       Sample docs for E1
            |    **/
            |    @SinceKotlin("1.3") // This annotation is transparent due to lack of @MustBeDocumented annotation
            |    E1
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = { m ->
                val entryNode = m.children.first { it.name == "testpackage" }.children.first { it.name == "TestEnum" }.children.firstIsInstance<ClasslikePageNode>()
                val signature = (entryNode.content as ContentGroup).dfs { it is ContentGroup && it.dci.toString() == "[testpackage/TestEnum.E1///PointingToDeclaration/{\"org.jetbrains.dokka.links.EnumEntryDRIExtra\":{\"key\":\"org.jetbrains.dokka.links.EnumEntryDRIExtra\"}}][Cover]" } as ContentGroup

                signature.assertNode {
                    header(1) { +"E1" }
                    platformHinted {
                        group {
                            group {
                                link { +"E1" }
                            }
                        }
                        group {
                            group {
                                group {
                                    +"Sample docs for E1"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Shouldn't try to give source links to synthetic methods (values, valueOf) if any are present
    // Initially reported for Java, making sure it doesn't fail for Kotlin either
    // https://github.com/Kotlin/dokka/issues/2544
    @Test
    fun `kotlin enum with configured source links should not fail the build due to synthetic methods`() {
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
            |/src/main/kotlin/basic/KotlinEnum.kt
            |package testpackage
            |
            |/**
            |* Doc
            |*/
            |enum class KotlinEnum {
            |    ONE, TWO, THREE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val sourceLink = writerPlugin.writer.renderedContent("root/testpackage/-kotlin-enum/index.html")
                    .select(".symbol .floating-right")
                    .select("a[href]")
                    .attr("href")

                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/kotlin/basic/KotlinEnum.kt#L6",
                    sourceLink
                )
            }
        }
    }
}
