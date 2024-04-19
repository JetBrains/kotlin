/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package filter

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.Invariance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KotlinArrayDocumentableReplacerTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
            }
        }
    }

    @Test
    fun `function with array type params`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun testFunction(param1: Array<Int>, param2: Array<Boolean>,
            |                 param3: Array<Float>, param4: Array<Double>,
            |                 param5: Array<Long>, param6: Array<Short>,
            |                 param7: Array<Char>, param8: Array<Byte>) { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val params = it.firstOrNull()?.packages?.firstOrNull()?.functions?.firstOrNull()?.parameters

                val typeArrayNames = listOf("IntArray", "BooleanArray", "FloatArray", "DoubleArray", "LongArray", "ShortArray",
                    "CharArray", "ByteArray")

                assertEquals(typeArrayNames.size, params?.size)
                params?.forEachIndexed{ i, param ->
                    assertEquals(GenericTypeConstructor(DRI("kotlin", typeArrayNames[i]), emptyList()),
                        param.type)
                }
            }
        }
    }
    @Test
    fun `function with specific parameters of array type`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun testFunction(param1: Array<Array<Int>>, param2: (Array<Int>) -> Array<Int>) { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val params = it.firstOrNull()?.packages?.firstOrNull()?.functions?.firstOrNull()?.parameters
                assertEquals(
                    Invariance(GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList())),
                    (params?.firstOrNull()?.type as? GenericTypeConstructor)?.projections?.firstOrNull())
                assertEquals(
                    Invariance(GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList())),
                    (params?.get(1)?.type as? FunctionalTypeConstructor)?.projections?.get(0))
                assertEquals(
                    Invariance(GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList())),
                    (params?.get(1)?.type as? FunctionalTypeConstructor)?.projections?.get(1))
            }
        }
    }
    @Test
    fun `property with array type`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |class MyTest { 
            |   val isEmpty: Array<Boolean>
            |       get() = emptyList
            |       set(value) {
            |           field = value
            |        }
            |}
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val myTestClass = it.firstOrNull()?.packages?.firstOrNull()?.classlikes?.firstOrNull()
                val property = myTestClass?.properties?.firstOrNull()

                assertEquals(GenericTypeConstructor(DRI("kotlin", "BooleanArray"), emptyList()),
                    property?.type)
                assertEquals(GenericTypeConstructor(DRI("kotlin", "BooleanArray"), emptyList()),
                    property?.getter?.type)
                assertEquals(GenericTypeConstructor(DRI("kotlin", "BooleanArray"), emptyList()),
                    property?.setter?.parameters?.firstOrNull()?.type)
            }
        }
    }
    @Test
    fun `typealias with array type`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |typealias arr = Array<Int>
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val arrTypealias = it.firstOrNull()?.packages?.firstOrNull()?.typealiases?.firstOrNull()

                assertEquals(GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList()),
                    arrTypealias?.underlyingType?.values?.firstOrNull())
            }
        }
    }

    // Unreal case: Upper bound of a type parameter cannot be an array
    @Test
    fun `generic fun and class`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun<T : Array<Int>> testFunction() { }
            |class myTestClass<T : Array<Int>>{ }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val testFun = it.firstOrNull()?.packages?.firstOrNull()?.functions?.firstOrNull()
                val myTestClass = it.firstOrNull()?.packages?.firstOrNull()?.classlikes?.firstOrNull() as? DClass

                assertEquals(GenericTypeConstructor(DRI("kotlin","IntArray"), emptyList()),
                    testFun?.generics?.firstOrNull()?.bounds?.firstOrNull())
                assertEquals(GenericTypeConstructor(DRI("kotlin","IntArray"), emptyList()),
                    myTestClass?.generics?.firstOrNull()?.bounds?.firstOrNull())
            }
        }
    }

    @Test
    fun `no jvm source set`() {
        val configurationWithNoJVM = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    name = "jvm"
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    name = "js"
                    sourceRoots = listOf("src/main/kotlin/basic/TestJS.kt")
                    analysisPlatform = "js"
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun testFunction(param: Array<Int>)
            |
            |
            |/src/main/kotlin/basic/TestJS.kt
            |package example
            |
            |fun testFunction(param: Array<Int>)
        """.trimMargin(),
            configurationWithNoJVM
        ) {
            preMergeDocumentablesTransformationStage = {
                val paramsJS = it[1].packages.firstOrNull()?.functions?.firstOrNull()?.parameters
                assertNotEquals(
                    GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList()),
                    paramsJS?.firstOrNull()?.type)

                val paramsJVM = it.firstOrNull()?.packages?.firstOrNull()?.functions?.firstOrNull()?.parameters
                assertEquals(
                    GenericTypeConstructor(DRI("kotlin", "IntArray"), emptyList()),
                    paramsJVM?.firstOrNull()?.type)
            }
        }
    }
}
