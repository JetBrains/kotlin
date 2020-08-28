/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.reflection

import org.junit.Test
import java.lang.reflect.*
import kotlin.reflect.javaType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress(
    "unused", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "RedundantInnerClassModifier", "NAME_SHADOWING",
    "FINAL_UPPER_BOUND"
)
class JavaTypeTest {
    @Test
    fun primitives() {
        assertEquals(Boolean::class.java, javaTypeOf<Boolean>())
        assertEquals(Char::class.java, javaTypeOf<Char>())
        assertEquals(Byte::class.java, javaTypeOf<Byte>())
        assertEquals(Short::class.java, javaTypeOf<Short>())
        assertEquals(Int::class.java, javaTypeOf<Int>())
        assertEquals(Float::class.java, javaTypeOf<Float>())
        assertEquals(Long::class.java, javaTypeOf<Long>())
        assertEquals(Double::class.java, javaTypeOf<Double>())
    }

    @Test
    fun primitiveArrays() {
        assertEquals(BooleanArray::class.java, javaTypeOf<BooleanArray>())
        assertEquals(CharArray::class.java, javaTypeOf<CharArray>())
        assertEquals(ByteArray::class.java, javaTypeOf<ByteArray>())
        assertEquals(ShortArray::class.java, javaTypeOf<ShortArray>())
        assertEquals(IntArray::class.java, javaTypeOf<IntArray>())
        assertEquals(FloatArray::class.java, javaTypeOf<FloatArray>())
        assertEquals(LongArray::class.java, javaTypeOf<LongArray>())
        assertEquals(DoubleArray::class.java, javaTypeOf<DoubleArray>())
    }

    @Test
    fun builtinClasses() {
        assertEquals(String::class.java, javaTypeOf<String>())
        assertEquals(Number::class.java, javaTypeOf<Number>())
        assertEquals(Unit::class.java, javaTypeOf<Unit>())
        assertEquals(Annotation::class.java, javaTypeOf<Annotation>())
        assertEquals(Cloneable::class.java, javaTypeOf<Cloneable>())
        assertEquals(String.Companion::class.java, javaTypeOf<String.Companion>())
        assertEquals(Deprecated::class.java, javaTypeOf<Deprecated>())

        assertEquals(Void::class.java, javaTypeOf<Void>())
    }

    @Test
    fun enumOfStar() {
        parameterized(javaTypeOf<Enum<*>>()) { type ->
            star(type.actualTypeArguments.single())
            assertEquals(null, type.ownerType)
            assertEquals(Enum::class.java, type.rawType)
            assertEquals("java.lang.Enum<?>", type.typeName)
        }

        assertEqualsAndHashCode(javaTypeOf<Enum<*>>(), javaTypeOf<Enum<*>>())
    }

    @Test
    fun recursiveEnumOfStar() {
        parameterized(javaTypeOf<Enum<out Enum<*>>>()) { type ->
            wildcardExtends(type.actualTypeArguments.single()) { starEnum ->
                parameterized(starEnum) { starEnumType ->
                    star(starEnumType.actualTypeArguments.single())
                    assertEquals(Enum::class.java, starEnumType.rawType)
                }
            }
            assertEquals(Enum::class.java, type.rawType)
            assertEquals("java.lang.Enum<? extends java.lang.Enum<?>>", type.typeName)
        }

        assertEqualsAndHashCode(javaTypeOf<Enum<out Enum<*>>>(), javaTypeOf<Enum<out Enum<*>>>())
    }

    @Test
    fun listOfString() {
        parameterized(javaTypeOf<List<String>>()) { type ->
            assertEquals(String::class.java, type.actualTypeArguments.single())
            assertEquals(null, type.ownerType)
            assertEquals(List::class.java, type.rawType)
            assertEquals("java.util.List<java.lang.String>", type.typeName)
        }
        parameterized(javaTypeOf<MutableList<String>>()) { type ->
            assertEquals(String::class.java, type.actualTypeArguments.single())
            assertEquals(null, type.ownerType)
            assertEquals(MutableList::class.java, type.rawType)
            assertEquals("java.util.List<java.lang.String>", type.typeName)
        }

        assertEqualsAndHashCode(javaTypeOf<List<String>>(), javaTypeOf<List<String>>())
    }

    @Test
    fun arrays() {
        assertEquals(Array<Any>::class.java, javaTypeOf<Array<Any>>())
        assertEquals(Array<Array<Any>>::class.java, javaTypeOf<Array<Array<Any>>>())
        assertEquals(Array<Boolean>::class.java, javaTypeOf<Array<Boolean>>())

        assertEquals(Array<Any>::class.java, javaTypeOf<Array<in String>>())
        assertEquals(Array<Any>::class.java, javaTypeOf<Array<in Array<String>>>())

        assertEquals(Array<Number>::class.java, javaTypeOf<Array<out Number>>())
        assertEquals(Array<Array<Number>>::class.java, javaTypeOf<Array<out Array<Number>>>())

        assertEquals(Array<Any>::class.java, javaTypeOf<Array<*>>())

        genericArray(javaTypeOf<Array<Set<String>>>()) { type ->
            parameterized(type.genericComponentType) { argument ->
                assertEquals(String::class.java, argument.actualTypeArguments.single())
                assertEquals(Set::class.java, argument.rawType)
            }
            assertEquals("java.util.Set<java.lang.String>[]", type.typeName)
        }

        assertEqualsAndHashCode(javaTypeOf<Array<Set<String>>>(), javaTypeOf<Array<Set<String>>>())
        assertNotEquals(javaTypeOf<Array<Set<String>>>(), javaTypeOf<Array<Collection<String>>>())
        assertNotEquals(javaTypeOf<Array<Set<String>>>(), javaTypeOf<Array<Set<CharSequence>>>())
        assertNotEquals(javaTypeOf<Array<Set<String>>>(), javaTypeOf<List<Set<String>>>())
    }

    @Test
    fun <U1, U2, U3 : U2> functionTypeVariables() where U2 : Number, U2 : Comparable<*> {
        val m = this::class.java.declaredMethods.single { it.name == "functionTypeVariables" }
        val tvs = m.typeParameters
        parameterized(javaTypeOf<Set<U1>>()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("U1", tv.name)
                assertEquals("U1", tv.typeName)
                assertEquals(listOf(Any::class.java), tv.bounds.toList())

                // assertEquals(m, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[0], tv)
            }
        }
        lateinit var u2: TypeVariable<*>
        parameterized(javaTypeOf<Set<U2>>()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("U2", tv.name)
                val bounds = tv.bounds.toList()
                assertEquals(2, bounds.size)
                assertEquals(Number::class.java, bounds[0])
                parameterized(bounds[1]) { comparable ->
                    assertEquals(Comparable::class.java, comparable.rawType)
                    star(comparable.actualTypeArguments.single())
                }
                u2 = tv

                // assertEquals(m, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[1], tv)
            }
        }
        parameterized(javaTypeOf<Set<U3>>()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("U3", tv.name)
                // assertEquals(listOf(u2), tv.bounds.toList())

                // assertEquals(m, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[2], tv)
            }
        }
    }

    private inner class T1<V1, V2, V3 : V2> where V2 : Number, V2 : Comparable<*> {
        fun setOfV1(): Type = javaTypeOf<Set<V1>>()
        fun setOfV2(): Type = javaTypeOf<Set<V2>>()
        fun setOfV3(): Type = javaTypeOf<Set<V3>>()
    }

    @Test
    fun classTypeVariables() {
        val t1 = T1<Int, Int, Int>()
        val tvs = T1::class.java.typeParameters
        parameterized(t1.setOfV1()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("V1", tv.name)
                assertEquals(listOf(Any::class.java), tv.bounds.toList())
                assertEquals("V1", tv.typeName)

                // assertEquals(T1::class.java, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[0], tv)
            }
        }
        lateinit var v2: TypeVariable<*>
        parameterized(t1.setOfV2()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("V2", tv.name)
                val bounds = tv.bounds.toList()
                assertEquals(2, bounds.size)
                assertEquals(Number::class.java, bounds[0])
                parameterized(bounds[1]) { comparable ->
                    assertEquals(Comparable::class.java, comparable.rawType)
                    star(comparable.actualTypeArguments.single())
                }
                v2 = tv

                // assertEquals(T1::class.java, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[1], tv)
            }
        }
        parameterized(t1.setOfV3()) { type ->
            typeVariable(type.actualTypeArguments.single()) { tv ->
                assertEquals("V3", tv.name)
                // assertEquals(listOf(v2), tv.bounds.toList())

                // assertEquals(T1::class.java, tv.genericDeclaration)
                // assertEqualsAndHashCode(tvs[2], tv)
            }
        }
    }

    private class T2<W>

    @Test
    fun useSiteVariance() {
        parameterized(javaTypeOf<T2<*>>()) { type ->
            star(type.actualTypeArguments.single())
        }
        parameterized(javaTypeOf<T2<out CharSequence>>()) { type ->
            wildcardExtends(type.actualTypeArguments.single()) { bound ->
                assertEquals(CharSequence::class.java, bound)
            }
        }
        parameterized(javaTypeOf<T2<in CharSequence>>()) { type ->
            wildcardSuper(type.actualTypeArguments.single()) { bound ->
                assertEquals(CharSequence::class.java, bound)
            }
        }

        assertEqualsAndHashCode(javaTypeOf<T2<*>>(), javaTypeOf<T2<*>>())
        assertEqualsAndHashCode(javaTypeOf<T2<out CharSequence>>(), javaTypeOf<T2<out CharSequence>>())
        assertEqualsAndHashCode(javaTypeOf<T2<in CharSequence>>(), javaTypeOf<T2<in CharSequence>>())

        assertNotEquals(javaTypeOf<T2<out CharSequence>>(), javaTypeOf<T2<out Number>>())
        assertNotEquals(javaTypeOf<T2<in CharSequence>>(), javaTypeOf<T2<in Number>>())
    }

    private class T3<X1, X2> {
        class Nested<Y>

        inner class Inner<Z> {
            inner class NonGeneric {
                inner class DeepInner<G1, G2>
            }
        }
    }

    @Test
    fun nestedTypes() {
        val nestedGenericType = javaTypeOf<T3.Nested<IntRange>>()
        parameterized(nestedGenericType) { type ->
            assertEquals(listOf(IntRange::class.java), type.actualTypeArguments.toList())
            assertEquals(T3::class.java, type.ownerType)
            assertEquals("test.reflection.JavaTypeTest\$T3\$Nested<kotlin.ranges.IntRange>", type.typeName)
        }
        assertEqualsAndHashCode(javaTypeOf<T3.Nested<String>>(), javaTypeOf<T3.Nested<String>>())
        assertNotEquals(javaTypeOf<T3.Nested<String>>(), javaTypeOf<T3.Nested<Int>>())


        val innerGenericType = javaTypeOf<T3<String, Unit>.Inner<IntRange>>()
        parameterized(innerGenericType) { type ->
            assertEquals(listOf(IntRange::class.java), type.actualTypeArguments.toList())
            parameterized(type.ownerType) { ownerType ->
                assertEquals(listOf(String::class.java, Unit::class.java), ownerType.actualTypeArguments.toList())
            }
            assertEquals("test.reflection.JavaTypeTest\$T3<java.lang.String, kotlin.Unit>\$Inner<kotlin.ranges.IntRange>", type.typeName)
        }
        assertEqualsAndHashCode(javaTypeOf<T3<String, Unit>.Inner<String>>(), javaTypeOf<T3<String, Unit>.Inner<String>>())
        assertNotEquals(javaTypeOf<T3<String, Unit>.Inner<String>>(), javaTypeOf<T3<String, Unit>.Inner<Int>>())
        assertNotEquals(javaTypeOf<T3<String, Unit>.Inner<String>>(), javaTypeOf<T3<String, String>.Inner<String>>())


        val deepInnerType = javaTypeOf<T3<Any, Int>.Inner<Char>.NonGeneric.DeepInner<Byte, Short>>()
        parameterized(deepInnerType) { deepInnerType ->
            assertEquals(listOf(Byte::class.javaObjectType, Short::class.javaObjectType), deepInnerType.actualTypeArguments.toList())
            parameterized(deepInnerType.ownerType) { nonGenericType ->
                assertEquals(emptyList(), nonGenericType.actualTypeArguments.toList())
                parameterized(nonGenericType.ownerType) { innerType ->
                    assertEquals(listOf(Char::class.javaObjectType), innerType.actualTypeArguments.toList())
                    parameterized(innerType.ownerType) { t3 ->
                        assertEquals(listOf(Any::class.java, Int::class.javaObjectType), t3.actualTypeArguments.toList())
                        assertEquals(JavaTypeTest::class.java, t3.ownerType)
                    }
                }
            }
            assertEquals(
                "test.reflection.JavaTypeTest\$T3<java.lang.Object, java.lang.Integer>\$Inner<java.lang.Character>" +
                        "\$NonGeneric\$DeepInner<java.lang.Byte, java.lang.Short>", deepInnerType.typeName
            )
        }
    }

    private inner class T4<I : Int> {
        fun setOfI(): Type = javaTypeOf<Set<I>>()
    }

    @Test
    fun primitiveUpperBound() {
        parameterized(T4<Int>().setOfI()) { setOfI ->
            typeVariable(setOfI.actualTypeArguments.single()) { i ->
                val bound = i.bounds.single()
                assertEquals(Int::class.javaObjectType, bound)
                assertNotEquals(Int::class.javaPrimitiveType, bound)
            }
        }
    }

    // -----

    private inline fun <reified T> javaTypeOf(): Type =
        typeOf<T>().javaType

    private fun <@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER") @kotlin.internal.OnlyInputTypes T> assertEqualsAndHashCode(expected: T, actual: T, message: String? = null) {
        assertEquals(expected, actual, message)
        assertEquals(actual, expected, message)
        assertEquals(expected.hashCode(), actual.hashCode(), message)
    }

    private inline fun <reified T : Type> checkIsType(type: Type, block: (T) -> Unit) {
        assertTrue(type is T, "Type is not ${T::class.java.simpleName}: $type (${type::class.java.name})")
        block(type)
    }

    private val Type.typeName: String
        // Calling getTypeName() via reflection because the interface method Type.getTypeName has only appeared in JDK 8.
        get() = (this::class.java.getDeclaredMethod("getTypeName").apply { isAccessible = true })(this) as String

    // A<X>
    private fun parameterized(type: Type, block: (ParameterizedType) -> Unit): Unit = checkIsType(type, block)

    // ?, ? extends ..., ? super ...
    private fun wildcard(type: Type, block: (WildcardType) -> Unit): Unit = checkIsType(type, block)

    // Array<A<X>>
    private fun genericArray(type: Type, block: (GenericArrayType) -> Unit): Unit = checkIsType(type, block)

    // T
    private fun typeVariable(type: Type, block: (TypeVariable<*>) -> Unit): Unit = checkIsType(type, block)

    // ? extends ...
    private fun wildcardExtends(type: Type, block: (upperBound: Type) -> Unit) {
        wildcard(type) { argument ->
            assertEquals(emptyList(), argument.lowerBounds.toList())
            val bound = argument.upperBounds.singleOrNull() ?: fail("Type is not an extends-wildcard: $type (${type::class.java.name})")
            block(bound)
        }
    }

    // ? super ...
    private fun wildcardSuper(type: Type, block: (lowerBound: Type) -> Unit) {
        wildcard(type) { argument ->
            assertEquals(listOf(Any::class.java), argument.upperBounds.toList())
            val bound = argument.lowerBounds.singleOrNull() ?: fail("Type is not a super-wildcard: $type (${type::class.java.name})")
            block(bound)
        }
    }

    // *
    private fun star(type: Type) {
        wildcardExtends(type) { bound ->
            assertEquals(Any::class.java, bound)
        }
        assertEquals("?", type.typeName)
    }
}
