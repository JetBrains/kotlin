/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-64460: When not producing cache, anonymous objects are extracted from inline functions to module scope, so the following happens
// kotlin.AssertionError: Expected <class codegen.kclass.kclass0.checkAnonymousObjects$$inlined$getHasFoo$1>, actual <class codegen.kclass.kclass0.MainKt$1>.
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST

package codegen.kclass.kclass0
import kotlin.test.*
import kotlin.reflect.KClass

interface HasFoo {
    fun foo(): String
}

private inline fun getHasFoo(s: String) = object : HasFoo {
    override fun foo(): String = s
}

fun box(): String {
    val hasFoo = getHasFoo("zzz")
    checkClass(
        hasFoo::class,
        expectedQualifiedName = null,
        expectedSimpleName = null, // KT-64460: simpleName is explicitly prohibited in NATIVE backend
        expectedToStringName = "class codegen.kclass.kclass0.box\$\$inlined\$getHasFoo\$1",
        expectedInstance = hasFoo,
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
