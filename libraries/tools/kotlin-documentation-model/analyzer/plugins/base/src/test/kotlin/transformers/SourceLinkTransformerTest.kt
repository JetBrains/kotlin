package transformers

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.assertEquals
import org.jsoup.nodes.Element

class SourceLinkTransformerTest : BaseAbstractTest() {

    private fun Element.getSourceLink() = select(".symbol .floating-right")
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
}