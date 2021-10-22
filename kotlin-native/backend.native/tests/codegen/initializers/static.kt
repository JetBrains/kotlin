/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package codegen.initializers.static

import kotlin.test.*
import kotlin.native.internal.*
import kotlin.reflect.*

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) : String {
        assertTrue(property.isPermanent());
        assertTrue(property.returnType.isPermanent())
        return property.name
    }
}

class A {
    val z by Delegate()
}

fun f() = 5

@Test fun testPermanent() {
    val x = typeOf<Map<String, Int>>()
    assertTrue(x.isPermanent())
    val a = A()
    assertTrue(a.z.isPermanent())
    assertEquals("z", a.z)
    val t = ::f
    assertTrue(t.isPermanent())
    assertEquals(5, t())
    val z = { 6 }
    assertTrue(z.isPermanent())
    assertEquals(6, z())
}

@Test fun testVarargChange() {
    fun varargGetter(position:Int, vararg x: Int): Int {
        x[position] *= 5;
        return x[position]
    }

    repeat(3) {
        assertEquals(10, varargGetter(0, 2, 3, 4))
        assertEquals(10, varargGetter(0, 2, 3, 4))
        assertEquals(15, varargGetter(1, 2, 3, 4))
        assertEquals(15, varargGetter(1, 2, 3, 4))
        assertEquals(20, varargGetter(2, 2, 3, 4))
        assertEquals(20, varargGetter(2, 2, 3, 4))
        assertFailsWith<ArrayIndexOutOfBoundsException> { varargGetter(3, 2, 3, 4) }
    }
}

@Test fun testArrays() {
    assertEquals("1, 2, 3", intArrayOf(1, 2, 3).joinToString())
    assertEquals("4, 5, 6", longArrayOf(4.toLong(), 5.toLong(), 6.toLong()).joinToString())
    assertEquals("7, 8, 9", shortArrayOf(7.toShort(), 8.toShort(), 9.toShort()).joinToString())
    assertEquals("10, 11, 12", byteArrayOf(10.toByte(), 11.toByte(), 12.toByte()).joinToString())
    assertEquals("abc", charArrayOf('a', 'b', 'c').joinToString(""))
    assertEquals("1.5, 2.5, -3.5", floatArrayOf(1.5f, 2.5f, -3.5f).joinToString())
    assertEquals("4.5, 5.5, -6.5", doubleArrayOf(4.5, 5.5, -6.5).joinToString())
    assertEquals("13, 14, 4294967295", uintArrayOf(13u, 14u, 4294967295u).joinToString())
    assertEquals("15, 16, 17", ulongArrayOf(15.toULong(), 16.toULong(), 17.toULong()).joinToString())
    assertEquals("18, 19, 40000", ushortArrayOf(18.toUShort(), 19.toUShort(), 40000.toUShort()).joinToString())
    assertEquals("20, 21, 200", ubyteArrayOf(20.toUByte(), 21.toUByte(), 200.toUByte()).joinToString())

    assertEquals("abc, def, ghi", arrayOf("abc", "def", "ghi").joinToString())
    assertEquals("1, 2, 3", arrayOf(1, 2, 3).joinToString())
    assertEquals("4, 5, 6", arrayOf(4.toLong(), 5.toLong(), 6.toLong()).joinToString())
    assertEquals("7, 8, 9", arrayOf(7.toShort(), 8.toShort(), 9.toShort()).joinToString())
    assertEquals("10, 11, 12", arrayOf(10.toByte(), 11.toByte(), 12.toByte()).joinToString())
    assertEquals("abc", arrayOf('a', 'b', 'c').joinToString(""))
    assertEquals("1.5, 2.5, -3.5", arrayOf(1.5f, 2.5f, -3.5f).joinToString())
    assertEquals("4.5, 5.5, -6.5", arrayOf(4.5, 5.5, -6.5).joinToString())
    assertEquals("13, 14, 4294967295", arrayOf(13u, 14u, 4294967295u).joinToString())
    assertEquals("15, 16, 17", arrayOf(15.toULong(), 16.toULong(), 17.toULong()).joinToString())
    assertEquals("18, 19, 40000", arrayOf(18.toUShort(), 19.toUShort(), 40000.toUShort()).joinToString())
    assertEquals("20, 21, 200", arrayOf(20.toUByte(), 21.toUByte(), 200.toUByte()).joinToString())

    assertEquals("abc, 1, 2, 3, 4, a, 1.5, 2.5, 5, 6, 7, 8",
            arrayOf("abc", 1, 2.toLong(), 3.toShort(), 4.toByte(), 'a', 1.5f, 2.5, 5u, 6.toULong(), 7.toUShort(), 8.toUByte()).joinToString())
}

@Test fun testList() {
    assertEquals("abc, def, ghi", listOf("abc", "def", "ghi").joinToString())
    assertEquals("1, 2, 3", listOf(1, 2, 3).joinToString())
    assertEquals("4, 5, 6", listOf(4.toLong(), 5.toLong(), 6.toLong()).joinToString())
    assertEquals("7, 8, 9", listOf(7.toShort(), 8.toShort(), 9.toShort()).joinToString())
    assertEquals("10, 11, 12", listOf(10.toByte(), 11.toByte(), 12.toByte()).joinToString())
    assertEquals("abc", listOf('a', 'b', 'c').joinToString(""))
    assertEquals("1.5, 2.5, -3.5", listOf(1.5f, 2.5f, -3.5f).joinToString())
    assertEquals("4.5, 5.5, -6.5", listOf(4.5, 5.5, -6.5).joinToString())
    assertEquals("13, 14, 4294967295", listOf(13u, 14u, 4294967295u).joinToString())
    assertEquals("15, 16, 17", listOf(15.toULong(), 16.toULong(), 17.toULong()).joinToString())
    assertEquals("18, 19, 40000", listOf(18.toUShort(), 19.toUShort(), 40000.toUShort()).joinToString())
    assertEquals("20, 21, 200", listOf(20.toUByte(), 21.toUByte(), 200.toUByte()).joinToString())

    assertEquals("abc, 1, 2, 3, 4, a, 1.5, 2.5, 5, 6, 7, 8",
            listOf("abc", 1, 2.toLong(), 3.toShort(), 4.toByte(), 'a', 1.5f, 2.5, 5u, 6.toULong(), 7.toUShort(), 8.toUByte()).joinToString())
}

@Test fun testKType() {
    val ktype = typeOf<Map<in String?, out List<*>>?>()
    assertTrue(ktype.isPermanent())
    assertEquals("Map", (ktype.classifier as? KClass<*>)?.simpleName)
    assertSame(Map::class, ktype.classifier)
    assertTrue(ktype.isMarkedNullable)
    assertTrue(ktype.arguments.isPermanent())
    assertEquals(2, ktype.arguments.size)
    assertSame(KVariance.IN, ktype.arguments[0].variance)
    assertSame(KVariance.OUT, ktype.arguments[1].variance)

    val arg0type = ktype.arguments[0].type!!
    assertTrue(arg0type.isPermanent())
    assertEquals("String", (arg0type.classifier as? KClass<*>)?.simpleName)
    assertSame(String::class, arg0type.classifier)
    assertTrue(arg0type.isMarkedNullable)
    assertTrue(arg0type.arguments.isPermanent())
    assertTrue(arg0type.arguments.isEmpty())

    val arg1type = ktype.arguments[1].type!!
    assertTrue(arg1type.isPermanent())
    assertEquals("List", (arg1type.classifier as? KClass<*>)?.simpleName)
    assertSame(List::class, arg1type.classifier)
    assertFalse(arg1type.isMarkedNullable)
    assertTrue(arg1type.arguments.isPermanent())
    assertTrue(arg1type.arguments.size == 1)
    assertSame(null, arg1type.arguments[0].variance)
    assertSame(null, arg1type.arguments[0].type)
}
class R<T, U, V, X>
interface S

@Test fun testReifiedKType() {

    inline fun <reified T, U, V> kTypeOf() where V : List<Int>, V : S, U : T = typeOf<R<T, in U, out V, *>>()

    class XX(val x:List<Int>) : List<Int> by x, S

    val type = kTypeOf<List<Int>, ArrayList<Int>, XX>()
    assertEquals("codegen.initializers.static.R<kotlin.collections.List<kotlin.Int>, in U, out V, *>", type.toString())
    assertEquals("[T]", (type.arguments[1].type!!.classifier as KTypeParameter).upperBounds.toString())
    assertEquals("[kotlin.collections.List<kotlin.Int>, codegen.initializers.static.S]",
            (type.arguments[2].type!!.classifier as KTypeParameter).upperBounds.toString())
}

inline fun invokeAndReturnKClass(block: ()->Boolean) : KClass<*> {
    try {
        if (block()) {
            return Double::class
        }
    } catch (e: Exception) {
        return String::class
    } finally {
        return Int::class
    }
}

@Test fun testConstantObjectInFinally() {
    for (i in 0..2) {
        val clazz = invokeAndReturnKClass {
            when (i) {
                0 -> true
                1 -> false
                else -> TODO("test")
            }
        }
        assertTrue(clazz.isPermanent())
        assertEquals("kotlin.Int", clazz.qualifiedName)
    }
}

@Test fun testSmallIntIdentity() {
    val xBool = true
    val xBoolStatic : Any = false
    val xBoolDyanmic : Any = !xBool
    assertSame(xBoolStatic, xBoolDyanmic)

    val xByte = 1.toByte()
    val xByteStatic : Any = 2.toByte()
    val xByteDyanmic : Any = (xByte + xByte).toByte()
    assertSame(xByteStatic, xByteDyanmic)

    val xShort = 1.toShort()
    val xShortStatic : Any = 2.toShort()
    val xShortDyanmic : Any = (xShort + xShort).toShort()
    assertSame(xShortStatic, xShortDyanmic)

    val xInt = 1.toInt()
    val xIntStatic : Any = 2.toInt()
    val xIntDyanmic : Any = xInt + xInt
    assertSame(xIntStatic, xIntDyanmic)

    val xChar = 1.toChar()
    val xCharStatic : Any = 2.toChar()
    val xCharDyanmic : Any = (xChar.code + xChar.code).toChar()
    assertSame(xCharStatic, xCharDyanmic)

    val xLong = 1.toLong()
    val xLongStatic = 2.toLong()
    val xLongDyanmic = xLong + xLong
    assertSame(xLongStatic, xLongDyanmic)
}