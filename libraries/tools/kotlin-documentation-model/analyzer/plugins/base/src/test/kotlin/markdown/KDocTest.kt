package markdown

import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.junit.jupiter.api.Assertions.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

abstract class KDocTest : AbstractCoreTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin/example/Test.kt")
            }
        }
    }

    private fun interpolateKdoc(kdoc: String) = """
            |/src/main/kotlin/example/Test.kt
            |package example
            | /**
            ${kdoc.split("\n").joinToString("") { "| *$it\n" } }
            | */
            |class Test
        """.trimMargin()

    private fun actualDocumentationNode(modulePageNode: ModulePageNode) =
        (modulePageNode.documentable?.children?.first() as DPackage)
            .classlikes.single()
            .documentation.values.single()


    protected fun executeTest(kdoc: String, expectedDocumentationNode: DocumentationNode) {
        testInline(
            interpolateKdoc(kdoc),
            configuration
        ) {
            pagesGenerationStage = {
                assertEquals(
                    expectedDocumentationNode,
                    actualDocumentationNode(it as ModulePageNode)
                )
            }
        }
    }
}
