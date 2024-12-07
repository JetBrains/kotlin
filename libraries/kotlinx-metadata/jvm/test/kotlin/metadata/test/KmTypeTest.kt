/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

typealias ListString = List<String>

class KmTypeTest {

    private fun KClass<*>.functionTypesByName(): Map<String, KmType> =
        java.readMetadataAsKmClass().functions.associateBy({ it.name }, { it.returnType })

    private fun testEq(one: KmType, other: KmType) {
        assertEquals(one, other)
        assertEquals(one.hashCode(), other.hashCode())
    }

    private fun Map<String, KmType>.shouldBeEqual(one: String, two: String) {
        testEq(get(one)!!, get(two)!!)
    }

    @Suppress("NAME_SHADOWING")
    private fun Map<String, KmType>.shouldBeNotEqual(one: String, two: String, allowHcCollision: Boolean = false) {
        val one = get(one)!!
        val two = get(two)!!
        assertNotEquals(one, two)
        if (!allowHcCollision) assertNotEquals(one.hashCode(), two.hashCode())
    }

    @Test
    fun testBasicEq() {
        class A {
            fun one(): String = ""
            fun two(): String = ""
        }
        A::class.functionTypesByName().shouldBeEqual("one", "two")
    }

    @Test
    fun testAttributesAreAccountedFor() {
        class A {
            fun string(): String = ""
            fun nullableString(): String? = ""
            fun nullableString2(): String? = null
            fun nonSuspendUnit(): () -> Unit = TODO()
            fun suspendUnit(): suspend () -> Unit = TODO()
            fun suspendUnit2(): suspend () -> Unit = TODO()
            fun nullableSuspend(): (suspend () -> Unit)? = null
        }

        with(A::class.functionTypesByName()) {
            shouldBeEqual("nullableString", "nullableString2")
            shouldBeNotEqual("string", "nullableString")
            shouldBeNotEqual("nonSuspendUnit", "suspendUnit")
            shouldBeEqual("suspendUnit", "suspendUnit2")
            shouldBeNotEqual("suspendUnit", "nullableSuspend")
        }
    }

    @Test
    fun testTypeArguments() {
        class A<Outer> {
            fun dnn(): List<Outer & Any> = TODO()
            fun param1(): List<Outer> = TODO()
            fun param2(): List<Outer> = TODO()
            fun paramNullable(): List<Outer?> = TODO()
            fun <Inner> inner(): List<Inner> = TODO()

            fun map1(): Map<String, Outer> = TODO()
            fun map2(): Map<String, Outer> = TODO()
            fun map3(): Map<Outer, String> = TODO()

            fun variance1(): MutableList<in Outer> = TODO()
            fun variance2(): MutableList<out Outer> = TODO()
            fun variance3(): MutableList<Outer> = TODO()
            fun variance4(): MutableList<*> = TODO()
        }

        val fs = A::class.functionTypesByName()
        with(fs) {
            shouldBeNotEqual("dnn", "param1")
            shouldBeEqual("param1", "param2")
            shouldBeNotEqual("param1", "paramNullable")

            shouldBeNotEqual("param1", "inner")

            shouldBeEqual("map1", "map2")
            shouldBeNotEqual("map1", "map3")

            for (i in 1..4) {
                for (j in i + 1..4)
                    shouldBeNotEqual("variance$i", "variance$j")
            }
        }
        // Check that Outer from different types is eq to itself
        val param1Arg = fs["param1"]!!.arguments.first().type!!
        val var3Arg = fs["variance3"]!!.arguments.first().type!!
        testEq(param1Arg, var3Arg)
    }

    // Cannot be local: KT-68602
    class A1<Outer> { inner class B<Inner> }
    class A2<Outer> { inner class B<Inner> }

    @Test
    fun testOuterTypes() {
        class A {
            fun a1bInt(): A1<*>.B<Int> = TODO()
            fun a1bInt2(): A1<*>.B<Int> = TODO()
            fun a2bInt(): A2<*>.B<Int> = TODO()

            fun a1NonStar(): A1<String>.B<Int> = TODO()
        }
        with(A::class.functionTypesByName()) {
            shouldBeEqual("a1bInt", "a1bInt2")
            shouldBeNotEqual("a1bInt", "a2bInt")
            shouldBeNotEqual("a1bInt", "a1NonStar", true)
        }
    }

    @Test
    fun testFlexibleUpperBound() {
        class A {
            fun java() /* (Mutable)List<Any!>! */ = JavaDeclaration.getList()
            fun mut(): MutableList<Any> = TODO()
            fun upper(): List<Any?> = TODO()
            fun javaObject() /* Any! */ = JavaDeclaration.getAny()
            fun str(): Any? = null

            fun javaRaw() /* (Mutable)List<*>! */ = JavaDeclaration.getRawList()
        }
        val fs = A::class.functionTypesByName()
        fs.shouldBeNotEqual("java", "mut", true)
        fs.shouldBeNotEqual("java", "upper")
        val javaArg = fs["java"]!!.arguments.first().type!!
        testEq(javaArg, fs["javaObject"]!!)

        fs.shouldBeNotEqual("javaObject", "str")
        val javaStrUpperBound = fs["javaObject"]!!.flexibleTypeUpperBound!!.type
        testEq(javaStrUpperBound, fs["str"]!!)

        // It seems that there are no other ways to get a star projection inside flexible upper bound,
        // so these types differ even if we do not check .isRaw JVM extension.
        fs.shouldBeNotEqual("java", "javaRaw")
    }

    @Test
    fun testEqualityAcrossDifferentMetadata() {
        class A {
            fun x(): List<String?> = TODO()
        }
        class B {
            fun x(): List<String?> = TODO()
        }
        val one = A::class.functionTypesByName()["x"]!!
        val two = B::class.functionTypesByName()["x"]!!
        testEq(one, two)
    }

    @Test
    fun testTypealiasedTypes() {
        class A {
            fun list(): List<String> = TODO()
            fun aliased(): ListString = TODO()
        }

        val fs = A::class.functionTypesByName()
        fs.shouldBeNotEqual("list", "aliased", allowHcCollision = true)

        val aliasedType: KmType = fs["aliased"]!!
        val abbr = aliasedType.abbreviatedType!!
        assertIs<KmClassifier.TypeAlias>(abbr.classifier)
    }

    @Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
    annotation class X
    @Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
    annotation class Y

    @Test
    fun testExtensionAnnotations() {
        class A {
            fun s(): String = ""
            fun x(): @X String = ""
            fun x2(): @X String = ""
            fun y(): @Y String = ""
            fun ls(): List<String> = TODO()
            fun lx(): List<@X String> = TODO()
        }

        val fs = A::class.functionTypesByName()
        fs.shouldBeNotEqual("s", "x", true)
        fs.shouldBeNotEqual("y", "x", true)
        fs.shouldBeEqual("x", "x2")

        fs.shouldBeNotEqual("ls", "lx", true)
    }
}
