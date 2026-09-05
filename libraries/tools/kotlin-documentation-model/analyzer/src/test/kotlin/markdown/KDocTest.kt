/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package markdown

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.DocumentationNode
import kotlin.test.assertEquals

abstract class KDocTest : BaseAbstractTest() {

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

    private fun actualDocumentationNode(module: DModule) =
        module.packages.single()
            .classlikes.single()
            .documentation.values.single()


    protected fun executeTest(kdoc: String, expectedDocumentationNode: DocumentationNode) {
        testInline(
            interpolateKdoc(kdoc),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    expectedDocumentationNode,
                    actualDocumentationNode(module)
                )
            }
        }
    }
}
