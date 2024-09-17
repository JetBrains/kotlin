/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.kclass.kclass0
import kotlin.test.*
import kotlin.reflect.KClass

interface HasFoo {
    fun foo(): String
}

private inline fun getHasFooPrivate(s: String) = object : HasFoo {
    override fun foo(): String = s + "_private"
}

inline fun getHasFooPublic(s: String) = object : HasFoo {
    override fun foo(): String = s + "_public"
}

fun box(): String {
    val hasFooPrivate = getHasFooPrivate("a")
    checkClass(
        hasFooPrivate::class,
        expectedQualifiedName = null,
        expectedSimpleName = null, // KT-64460: simpleName is explicitly prohibited in NATIVE backend
        expectedToStringName = "class codegen.kclass.kclass0.box\$\$inlined\$getHasFooPrivate\$1",
        expectedInstance = hasFooPrivate,
        expectedNotInstance = Any()
    )
    val hasFooPublic = getHasFooPublic("b")
    checkClass(
        hasFooPublic::class,
        expectedQualifiedName = null,
        expectedSimpleName = null, // KT-64460: simpleName is explicitly prohibited in NATIVE backend
        expectedToStringName = "class codegen.kclass.kclass0.box\$\$inlined\$getHasFooPublic\$1",
        expectedInstance = hasFooPublic,
        expectedNotInstance = Any()
    )
    return "OK"
}

private fun checkClass(
    clazz: KClass<*>,
    expectedQualifiedName: String?, expectedSimpleName: String?, expectedToStringName: String,
    expectedInstance: Any, expectedNotInstance: Any?
) {
    assertEquals(expectedQualifiedName, clazz.qualifiedName)
    assertEquals(expectedSimpleName, clazz.simpleName)
    assertEquals(expectedToStringName, clazz.toString())

    assertTrue(clazz.isInstance(expectedInstance))
    if (expectedNotInstance != null) assertTrue(!clazz.isInstance(expectedNotInstance))
}
