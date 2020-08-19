package translators

import org.junit.jupiter.api.Assertions.*
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test

class DefaultDescriptorToDocumentableTranslatorTest : AbstractCoreTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `data class kdocs over generated methods`() {
        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |data class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assert(module.documentationOf("XD", "copy") == "")
                assert(
                    module.documentationOf(
                        "XD",
                        "equals"
                    ) == "Memory is not what the heart desires. That is only a mirror."
                )
                assert(module.documentationOf("XD", "hashCode") == "")
                assert(module.documentationOf("XD", "toString") == "")
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
            }
        }
    }

    @Test
    fun `simple class kdocs`() {
        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
                assert(
                    module.documentationOf(
                        "XD",
                        "equals"
                    ) == "Memory is not what the heart desires. That is only a mirror."
                )
            }
        }
    }

    @Test
    fun `kdocs with code block`() {
        testInline(
            """
            |/src/main/kotlin/sample/TestForCodeInDocs.kt
            |package sample
            |/**
            | * Utility for building a String that represents an XML document.
            | * The XmlBlob object is immutable and the passed values are copied where it makes sense.
            | *
            | * Note the XML Declaration is not output as part of the XmlBlob
            | *
            | * 
            | *    val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
            | *        "soap-env:encodingStyle" to "http://www.w3.org/2001/12/soap-encoding")
            | *    val soapXml = node("soap-env:Envelope", soapAttrs,
            | *        node("soap-env:Body", attrs("xmlns:m" to "http://example"),
            | *            node("m:GetExample",
            | *                node("m:GetExampleName", "BasePair")
            | *            )
            | *        )
            | *    )
            | *
            | *
            | */
            |class TestForCodeInDocs {
            |}
        """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val description = module.descriptionOf("TestForCodeInDocs")
                val expected = listOf(
                    P(
                        children = listOf(Text("Utility for building a String that represents an XML document. The XmlBlob object is immutable and the passed values are copied where it makes sense."))
                    ),
                    P(
                        children = listOf(Text("Note the XML Declaration is not output as part of the XmlBlob"))
                    ),
                    CodeBlock(
                        children = listOf(
                            Text(
                                """    val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
        "soap-env:encodingStyle" to "http://www.w3.org/2001/12/soap-encoding")
    val soapXml = node("soap-env:Envelope", soapAttrs,
        node("soap-env:Body", attrs("xmlns:m" to "http://example"),
            node("m:GetExample",
                node("m:GetExampleName", "BasePair")
            )
        )
    )"""
                            )
                        )
                    )
                )
                assertEquals(expected, description?.root?.children)
            }
        }
    }
}