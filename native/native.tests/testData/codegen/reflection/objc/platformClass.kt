// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package mypackage

import kotlin.reflect.KClass
import kotlin.test.*
import platform.Foundation.NSUUID
import platform.darwin.NSObject

val kclass = NSUUID::class

inline fun <reified T: Any> getKClass(): KClass<*> = T::class

inline fun <T> assertEquals2Way(lhs: T, rhs: T) {
    assertEquals(lhs, rhs)
    assertEquals(rhs, lhs)
    assertEquals(lhs.hashCode(), rhs.hashCode())
}

fun box(): String {
    val instance = NSUUID()

    val instanceKClass = instance::class

    // Getting KClass
    assertEquals2Way(kclass, getKClass<NSUUID>())
    // Not checking instance::class, because it may be of a different class

    // isInstance
    assertTrue(kclass.isInstance(instance))
    assertFalse(kclass.isInstance(null))
    assertFalse(kclass.isInstance(NSObject()))

    assertTrue(instanceKClass.isInstance(instance))
    assertFalse(instanceKClass.isInstance(null))
    assertFalse(instanceKClass.isInstance(NSObject()))

    // names
    assertEquals("NSUUID", kclass.simpleName)
    assertNull(kclass.qualifiedName)
    assertEquals("class NSUUID", kclass.toString())

    return "OK"
}