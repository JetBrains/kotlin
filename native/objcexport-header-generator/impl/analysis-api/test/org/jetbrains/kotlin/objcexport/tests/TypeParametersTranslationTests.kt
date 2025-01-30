package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCGenericTypeParameterUsage
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCIdType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolType
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.testUtils.*
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeParametersTranslationTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - upper bound class parameter translated as generic`() {
        doTestOnProperty(
            """
            interface UpperBound
            class Foo<T : UpperBound>(val bar: T)
        """.trimIndent()
        ) { property ->
            val objCType = mapToReferenceTypeIgnoringNullability(property.returnType)
            assertEquals("T", (objCType as ObjCGenericTypeParameterUsage).typeName)
        }
    }

    @Test
    fun `test - class parameter translated as generic`() {
        doTestOnProperty(
            """
            class Foo<Bar> {
                val bar: Bar
            }
        """
        ) { property ->
            val objCType = mapToReferenceTypeIgnoringNullability(property.returnType)
            assertEquals("Bar", (objCType as ObjCGenericTypeParameterUsage).typeName)
        }
    }

    @Test
    fun `test - interface class parameter translated as id`() {
        doTestOnProperty(
            """
            interface Foo<Bar> {
                val bar: Bar
            }
        """
        ) { property ->
            val objCType = mapToReferenceTypeIgnoringNullability(property.returnType)
            assertTrue(objCType is ObjCIdType)
        }
    }

    @Test
    fun `test - callable class parameter without upper bound translated as generic`() {
        doTestOnMethod(
            """
            class Foo<Bar> {
                fun bar(t: Bar) 
            }
        """
        ) { method ->
            val objCType = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            assertEquals("Bar", (objCType as ObjCGenericTypeParameterUsage).typeName)
        }
    }

    @Test
    fun `test - callable parameter translated as id`() {
        doTestOnMethod(
            """
            class Foo {
                fun <T> bar(t: T) 
            }
        """
        ) { method ->
            val objCType = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            assertTrue(objCType is ObjCIdType)
        }
    }

    @Test
    fun `test - callable parameter with upper bound translated as upper bound`() {
        doTestOnMethod(
            """
                interface UpperBound
                
                class Foo {
                    fun <T> bar(t: T) where T : UpperBound = Unit
                }
        """
        ) { method ->
            val objCType = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            assertEquals("UpperBound", (objCType as ObjCProtocolType).protocolName)
        }
    }

    @Test
    fun `test - multiple class parameters with upper bounds translated as upper bounds`() {
        doTestOnMethod(
            """
                interface UpperBoundA
                interface UpperBoundB
                
                class Foo<A, B> where A : UpperBoundA, B : UpperBoundB {
                    fun bar(a: A, b: B) = Unit
                }
        """
        ) { method ->
            val objCTypeA = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            val objCTypeB = mapToReferenceTypeIgnoringNullability(method.valueParameters.second().returnType)
            assertEquals("A", (objCTypeA as ObjCGenericTypeParameterUsage).typeName)
            assertEquals("B", (objCTypeB as ObjCGenericTypeParameterUsage).typeName)
        }
    }

    @Test
    fun `test - multiple callable parameters with upper bounds translated as upper bounds`() {
        doTestOnMethod(
            """
                interface UpperBoundA
                interface UpperBoundB
                
                class Foo {
                    fun <A, B> bar(a: A, b: B) where A : UpperBoundA, B : UpperBoundB = Unit
                }
        """
        ) { method ->
            val objCTypeA = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            val objCTypeB = mapToReferenceTypeIgnoringNullability(method.valueParameters.second().returnType)
            assertEquals("UpperBoundA", (objCTypeA as ObjCProtocolType).protocolName)
            assertEquals("UpperBoundB", (objCTypeB as ObjCProtocolType).protocolName)
        }
    }

    @Test
    fun `test - class type parameter with chain of upper bounds translated as generic`() {
        doTestOnProperty(
            """
                interface UpperBound0
                interface UpperBound1 : UpperBound0
                interface UpperBound2 : UpperBound1
                
                class Foo<ClassParameter>(val bar: ClassParameter) where ClassParameter : UpperBound2 
        """
        ) { property ->
            val objCType = mapToReferenceTypeIgnoringNullability(property.returnType)
            assertEquals("ClassParameter", (objCType as ObjCGenericTypeParameterUsage).typeName)
        }
    }

    @Test
    fun `test - method type parameter with chain of upper bounds translated as generic`() {
        doTestOnMethod(
            """
                interface UpperBound0
                interface UpperBound1 : UpperBound0
                interface UpperBound2 : UpperBound1
                
                class Foo {
                    fun <T> bar(t: T) where T : UpperBound2 = Unit
                } 
        """
        ) { method ->
            val objCType = mapToReferenceTypeIgnoringNullability(method.valueParameters.first().returnType)
            assertEquals("UpperBound2", (objCType as ObjCProtocolType).protocolName)
        }
    }

    @Test
    fun `test - classifier context case when same parameter type is translated differently `() {
        doTest(
            """
            interface UpperBound
            open class Foo<T : UpperBound>(val t: T)
            class Bar : Foo<UpperBound>(t = null!!)
        """
        ) { file ->
            val foo = analysisSession.getClassOrFail(file, "Foo")
            val bar = analysisSession.getClassOrFail(file, "Bar")

            val fooObjC = translateToObjCClass(foo)
            val barObjC = translateToObjCClass(bar)

            val initFoo = fooObjC?.members?.first { it.name.startsWith("initWith") } as? ObjCMethod
                ?: error("no initWith constructor were translate for Foo")
            val initBar = barObjC?.members?.first { it.name.startsWith("initWith") } as? ObjCMethod
                ?: error("no initWith constructor were translate for Bar")

            assertEquals("t", initFoo.parameters.first().name)
            assertEquals("T", (initFoo.parameters.first().type as ObjCGenericTypeParameterUsage).typeName)

            assertEquals("t", initBar.parameters.first().name)
            assertEquals("UpperBound", (initBar.parameters.first().type as ObjCProtocolType).protocolName)
        }
    }

    private fun doTestOnProperty(@Language("kotlin") code: String, run: ObjCExportContext.(property: KaPropertySymbol) -> Unit) {
        doTest(code) { file ->
            val foo = analysisSession.getClassOrFail(file, "Foo")
            val property = with(analysisSession) { foo.declaredMemberScope.getPropertyOrFail("bar") }
            run(property)
        }
    }

    private fun doTestOnMethod(@Language("kotlin") code: String, run: ObjCExportContext.(property: KaNamedFunctionSymbol) -> Unit) {
        doTest(code) { file ->
            val foo = analysisSession.getClassOrFail(file, "Foo")
            val func = with(analysisSession) { foo.declaredMemberScope.getFunctionOrFail("bar") }
            run(func)
        }
    }

    private fun doTest(@Language("kotlin") code: String, run: ObjCExportContext.(file: KtFile) -> Unit) {
        val file = inlineSourceCodeAnalysis.createKtFile(code)
        analyze(file) {
            with(
                ObjCExportContext(
                    analysisSession = this,
                    exportSession = KtObjCExportSessionImpl(
                        KtObjCExportConfiguration(),
                        moduleNaming = KtObjCExportModuleNaming.default,
                        moduleClassifier = KtObjCExportModuleClassifier.default,
                        cache = hashMapOf(),
                        overrides = hashMapOf()
                    )
                )
            ) {
                run(file)
            }
        }
    }
}