/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.jvm.annotations
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@Target(AnnotationTarget.TYPE)
internal annotation class MyAnn(val s: String, val nested: MyAnnNested, val kClass: WithKClass)

@Target(AnnotationTarget.TYPE)
internal annotation class WithKClass(val kClass: KClass<*>)

internal annotation class MyAnnNested(val e: E, val a: Array<String>)

internal enum class E { A, B }
class AnnotationsStringFormTest {

    private fun doTest(argumentToExpected: Map<KmAnnotationArgument, String>) {
        val valuesArgMap = argumentToExpected.keys.mapIndexed { i, km ->
            "value$i" to km
        }.toMap()
        val valuesExpected = argumentToExpected.values.withIndex().joinToString { (i, s) -> "value$i = $s" }
        val anno = KmAnnotation("com/my/Foo", valuesArgMap)
        assertEquals("@com/my/Foo($valuesExpected)", anno.toString())
    }

    @Test
    fun testSignedLiteralValues() {
        val values: Map<KmAnnotationArgument, String> = mapOf(
            KmAnnotationArgument.ByteValue(1) to "ByteValue(1)",
            KmAnnotationArgument.ShortValue(2) to "ShortValue(2)",
            KmAnnotationArgument.IntValue(3) to "IntValue(3)",
            KmAnnotationArgument.LongValue(4) to "LongValue(4)",
        )
        doTest(values)
    }

    @Test
    fun testUnsignedLiteralValues() {
        val values: Map<KmAnnotationArgument, String> = mapOf(
            KmAnnotationArgument.UByteValue(1u) to "UByteValue(1)",
            KmAnnotationArgument.UShortValue(2u) to "UShortValue(2)",
            KmAnnotationArgument.UIntValue(3u) to "UIntValue(3)",
            KmAnnotationArgument.ULongValue(4u) to "ULongValue(4)",
        )
        doTest(values)
    }

    @Test
    fun testOtherLiteralValues() {
        val values: Map<KmAnnotationArgument, String> = mapOf(
            KmAnnotationArgument.StringValue("foo") to "StringValue(\"foo\")",
            KmAnnotationArgument.CharValue('a') to "CharValue(a)",
            KmAnnotationArgument.FloatValue(2.1f) to "FloatValue(2.1)",
            KmAnnotationArgument.DoubleValue(3.3) to "DoubleValue(3.3)",
            KmAnnotationArgument.BooleanValue(true) to "BooleanValue(true)",
        )
        doTest(values)
    }

    @Test
    fun testNonLiteralValues() {
        val values = mapOf(
            KmAnnotationArgument.EnumValue("foo/bar", "BAZ") to "EnumValue(foo/bar.BAZ)",
            KmAnnotationArgument.AnnotationValue(KmAnnotation("com/my/Bar", mapOf())) to "AnnotationValue(@com/my/Bar())",
            KmAnnotationArgument.ArrayValue(
                listOf(
                    KmAnnotationArgument.IntValue(1),
                    KmAnnotationArgument.IntValue(2),
                    KmAnnotationArgument.IntValue(3)
                )
            ) to "ArrayValue([IntValue(1), IntValue(2), IntValue(3)])",
            KmAnnotationArgument.KClassValue("com/my/Bar") to "KClassValue(com/my/Bar)",
            KmAnnotationArgument.ArrayKClassValue("com/my/Bar", 1) to "ArrayKClassValue(kotlin/Array<com/my/Bar>)"
        )
        doTest(values)
    }

    @Test
    fun testIrlValues() {
        // Annotations are stored directly in metadata only for types and type aliases
        class Holder(val e: @MyAnn("foo", MyAnnNested(E.B, arrayOf("a", "b", "c")), WithKClass(E::class)) String)

        val md = Holder::class.java.readMetadataAsKmClass()
        val annotation = md.properties.first().returnType.annotations.single()
        assertEquals(
            "@kotlin/metadata/test/MyAnn(" +
                    "s = StringValue(\"foo\"), " +
                    "nested = AnnotationValue(@kotlin/metadata/test/MyAnnNested(e = EnumValue(kotlin/metadata/test/E.B), " +
                    "a = ArrayValue([StringValue(\"a\"), StringValue(\"b\"), StringValue(\"c\")]))), " +
                    "kClass = AnnotationValue(@kotlin/metadata/test/WithKClass(kClass = KClassValue(kotlin/metadata/test/E)))" +
                    ")",
            annotation.toString()
        )
    }

    @Test
    fun testArrayKClassValue() {
        class Holder(val e: @WithKClass(Array<E>::class) String)

        val md = Holder::class.java.readMetadataAsKmClass()
        val annotation = md.properties.first().returnType.annotations.single()
        assertEquals("@kotlin/metadata/test/WithKClass(kClass = ArrayKClassValue(kotlin/Array<kotlin/metadata/test/E>))", annotation.toString())
    }
}
