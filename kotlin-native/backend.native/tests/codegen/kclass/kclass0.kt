/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.kclass.kclass0

import kotlin.test.*
import kotlin.reflect.KClass

@Test fun runTest() {
    main(emptyArray<String>())
}

fun main(args: Array<String>) {
    checkClass(Any::class, "kotlin.Any", "Any", Any(), null)
    checkClass(Int::class, "kotlin.Int", "Int", 42, "17")
    checkClass(String::class, "kotlin.String", "String", "17", 42)
    checkClass(RootClass::class, "codegen.kclass.kclass0.RootClass", "RootClass", RootClass(), Any())
    checkClass(RootClass.Nested::class, "codegen.kclass.kclass0.RootClass.Nested", "Nested", RootClass.Nested(), Any())

    class Local {
        val captured = args

        inner class Inner
    }
    checkClass(Local::class, null, "Local", Local(), Any())
    checkClass(Local.Inner::class, null, "Inner", Local().Inner(), Any())

    val obj = object : Any() {
        val captured = args

        inner class Inner
        val innerKClass = Inner::class
    }
    checkClass(obj::class, null, null, obj, Any())
    checkClass(obj.innerKClass, null, "Inner", obj.Inner(), Any())

    // Interfaces:
    checkClass(Comparable::class, "kotlin.Comparable", "Comparable", 42, Any())
    checkClass(Interface::class, "codegen.kclass.kclass0.Interface", "Interface", object : Interface {}, Any())

    checkInstanceClass(Any(), Any::class)
    checkInstanceClass(42, Int::class)
    assert(42::class == Int::class)

    checkReifiedClass<Int>(Int::class)
    checkReifiedClass<Int?>(Int::class)
    checkReifiedClass2<Int>(Int::class)
    checkReifiedClass2<Int?>(Int::class)
    checkReifiedClass<Any>(Any::class)
    checkReifiedClass2<Any>(Any::class)
    checkReifiedClass2<Any?>(Any::class)
    checkReifiedClass<Local>(Local::class)
    checkReifiedClass2<Local>(Local::class)
    checkReifiedClass<RootClass>(RootClass::class)
    checkReifiedClass2<RootClass>(RootClass::class)
}

class RootClass {
    class Nested
}
interface Interface

fun checkClass(
        clazz: KClass<*>,
        expectedQualifiedName: String?, expectedSimpleName: String?,
        expectedInstance: Any, expectedNotInstance: Any?
) {
    assert(clazz.qualifiedName == expectedQualifiedName)
    assert(clazz.simpleName == expectedSimpleName)

    assert(clazz.isInstance(expectedInstance))
    if (expectedNotInstance != null) assert(!clazz.isInstance(expectedNotInstance))
}

fun checkInstanceClass(instance: Any, clazz: KClass<*>) {
    assert(instance::class == clazz)
}

inline fun <reified T> checkReifiedClass(expectedClass: KClass<*>) {
    assert(T::class == expectedClass)
}

inline fun <reified T> checkReifiedClass2(expectedClass: KClass<*>) {
    checkReifiedClass<T>(expectedClass)
    checkReifiedClass<T?>(expectedClass)
}
