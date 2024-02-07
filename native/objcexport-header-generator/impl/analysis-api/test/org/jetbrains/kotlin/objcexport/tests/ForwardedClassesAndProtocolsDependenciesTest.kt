package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.translateToObjCHeader
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests logic of filtering protocols and classes which must be forwarded or excluded when dependency is used
 * For full tests of dependencies see [ObjCDependenciesTypesTest]
 */
class ForwardedClassesAndProtocolsDependenciesTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - iterator`() {
        doTest(
            code = """
            val i: Iterator<Int>
            """,
            protocols = setOf("Iterator"),
            classes = emptySet()
        )
    }

    @Test
    fun `test - array`() {
        doTest(
            code = """
            val i: Array<Int>
            """,
            protocols = setOf("Iterator"),
            classes = setOf("Array")
        )
    }

    @Test
    fun `test - string builder`() {
        doTest(
            code = """
            val i: StringBuilder
            """,
            protocols = setOf("CharSequence", "Appendable", "Iterator"),
            classes = setOf("StringBuilder", "CharArray", "CharIterator")
        )
    }

    @Test
    fun `test -  declared symbols`() {
        doTest(
            code = """
            open class ClassA
            class ClassB: ClassA()
            interface InterfaceA
            interface InterfaceB : InterfaceA()
            fun getClass(): ClassB = error("error")
            fun getInterface(): InterfaceB = error("error")
            """,
            protocols = setOf("InterfaceB", "InterfaceA"),
            classes = setOf("ClassB", "ClassA")
        )
    }

    private fun doTest(
        @Language("kotlin") code: String,
        protocols: Set<String>,
        classes: Set<String>,
    ) {
        val file = inlineSourceCodeAnalysis.createKtFile(code.trimIndent())
        val classesAndProtocols = translateClassesAndProtocols(file)

        assertEquals(protocols, classesAndProtocols.protocolForwardDeclarations.toSet(), "Invalid protocols set")
        assertEquals(classes, classesAndProtocols.classForwardDeclarations.map { it.className }.toSet(), "Invalid classes set")
    }

    private fun translateClassesAndProtocols(file: KtFile): ObjCHeader {
        return analyze(file) {
            KtObjCExportSession(KtObjCExportConfiguration()) {
                translateToObjCHeader(listOf(file))
            }
        }
    }
}