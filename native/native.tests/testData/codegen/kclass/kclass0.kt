/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package codegen.kclass.kclass0
import kotlin.test.*
import kotlin.coroutines.*
import kotlin.reflect.KClass

fun box(): String {
    main(emptyArray<String>())
    return "OK"
}

fun main(args: Array<String>) {
    checkBasics()
    checkLocalClasses(args)
    checkAnonymousObjects(args)
    checkAnonymousObjectsAssignedToProperty(args)
    checkFunctionReferences()
    checkInterfaces()
    checkEtc()
}

private fun checkBasics() {
    checkClass(
            clazz = Any::class,
            expectedQualifiedName = "kotlin.Any",
            expectedSimpleName = "Any",
            expectedToStringName = "class kotlin.Any",
            expectedInstance = Any(),
            expectedNotInstance = null
    )
    checkClass(
            clazz = Int::class,
            expectedQualifiedName = "kotlin.Int",
            expectedSimpleName = "Int",
            expectedToStringName = "class kotlin.Int",
            expectedInstance = 42,
            expectedNotInstance = "17"
    )
    checkClass(
            clazz = String::class,
            expectedQualifiedName = "kotlin.String",
            expectedSimpleName = "String",
            expectedToStringName = "class kotlin.String",
            expectedInstance = "17",
            expectedNotInstance = 42
    )
    checkClass(
            clazz = TopLevel::class,
            expectedQualifiedName = "codegen.kclass.kclass0.TopLevel",
            expectedSimpleName = "TopLevel",
            expectedToStringName = "class codegen.kclass.kclass0.TopLevel",
            expectedInstance = TopLevel(),
            expectedNotInstance = Any()
    )
    checkClass(
            clazz = TopLevel.Nested::class,
            expectedQualifiedName = "codegen.kclass.kclass0.TopLevel.Nested",
            expectedSimpleName = "Nested",
            expectedToStringName = "class codegen.kclass.kclass0.TopLevel.Nested",
            expectedInstance = TopLevel.Nested(),
            expectedNotInstance = Any()
    )
}

private fun checkLocalClasses(args: Array<String>) {
    // Local class.
    class Local0 {
        val captured = args
        inner class Inner
    }
    checkClass(
            clazz = Local0::class,
            expectedQualifiedName = null,
            expectedSimpleName = "Local0",
            expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$Local0",
            expectedInstance = Local0(),
            expectedNotInstance = Any()
    )
    checkClass(
            clazz = Local0.Inner::class,
            expectedQualifiedName = null,
            expectedSimpleName = "Inner",
            expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$Local0\$Inner",
            expectedInstance = Local0().Inner(),
            expectedNotInstance = Any()
    )

    // Local class inside of lambda.
    ;{
        class Local {
            val captured = args
            inner class Inner
        }
        checkClass(
                clazz = Local::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Local",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$1\$Local",
                expectedInstance = Local(),
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = Local.Inner::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$1\$Local\$Inner",
                expectedInstance = Local().Inner(),
                expectedNotInstance = Any()
        )
    }.invoke()

    // Local class inside of non-inlined lambda.
    runNonInlining {
        class Local {
            val captured = args
            inner class Inner
        }
        checkClass(
                clazz = Local::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Local",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$2\$Local",
                expectedInstance = Local(),
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = Local.Inner::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$2\$Local\$Inner",
                expectedInstance = Local().Inner(),
                expectedNotInstance = Any()
        )
    }

    // Local class inside of inlined lambda.
    run {
        class Local {
            val captured = args
            inner class Inner
        }
        checkClass(
                clazz = Local::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Local",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$Local",
                expectedInstance = Local(),
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = Local.Inner::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$Local\$Inner",
                expectedInstance = Local().Inner(),
                expectedNotInstance = Any()
        )
    }

    // Local class inside of suspend lambda.
    suspend {
        class Local {
            val captured = args
            inner class Inner
        }
        checkClass(
                clazz = Local::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Local",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$3\$Local",
                expectedInstance = Local(),
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = Local.Inner::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$3\$Local\$Inner",
                expectedInstance = Local().Inner(),
                expectedNotInstance = Any()
        )
    }.runCoroutine()

    // Local class inside of suspend function.
    suspend fun suspendFunWithLocalClass() {
        class Local {
            val captured = args
            inner class Inner
        }
        checkClass(
                clazz = Local::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Local",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$suspendFunWithLocalClass\$Local",
                expectedInstance = Local(),
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = Local.Inner::class,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkLocalClasses\$suspendFunWithLocalClass\$Local\$Inner",
                expectedInstance = Local().Inner(),
                expectedNotInstance = Any()
        )
    }
    ::suspendFunWithLocalClass.runCoroutine()
}

private fun checkAnonymousObjects(args: Array<String>) {
    // Anonymous object.
    with(object : Any() {
        val captured = args
        inner class Inner
        val innerKClass = Inner::class
    }) {
        checkClass(
                clazz = this::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$1",
                expectedInstance = this,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = this.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$1\$Inner",
                expectedInstance = this.Inner(),
                expectedNotInstance = Any()
        )
    }

    // Anonymous object inside of lambda.
    ;{
        // $2 goes for this lambda itself.

        with(object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }) {
            checkClass(
                    clazz = this::class,
                    expectedQualifiedName = null,
                    expectedSimpleName = null,
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$2\$1",
                    expectedInstance = this,
                    expectedNotInstance = Any()
            )
            checkClass(
                    clazz = this.innerKClass,
                    expectedQualifiedName = null,
                    expectedSimpleName = "Inner",
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$2\$1\$Inner",
                    expectedInstance = this.Inner(),
                    expectedNotInstance = Any()
            )
        }
    }.invoke()

    // Anonymous object inside of non-inlined lambda.
    runNonInlining {
        with(object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }) {
            checkClass(
                    clazz = this::class,
                    expectedQualifiedName = null,
                    expectedSimpleName = null,
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$3\$1",
                    expectedInstance = this,
                    expectedNotInstance = Any()
            )
            checkClass(
                    clazz = this.innerKClass,
                    expectedQualifiedName = null,
                    expectedSimpleName = "Inner",
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$3\$1\$Inner",
                    expectedInstance = this.Inner(),
                    expectedNotInstance = Any()
            )
        }
    }

    // Anonymous object inside of inlined lambda.
    run {
        with(object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }) {
            checkClass(
                    clazz = this::class,
                    expectedQualifiedName = null,
                    expectedSimpleName = null,
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$4",
                    expectedInstance = this,
                    expectedNotInstance = Any()
            )
            checkClass(
                    clazz = this.innerKClass,
                    expectedQualifiedName = null,
                    expectedSimpleName = "Inner",
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$4\$Inner",
                    expectedInstance = this.Inner(),
                    expectedNotInstance = Any()
            )
        }
    }

    // Anonymous object inside of suspend lambda.
    suspend {
        with(object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }) {
            checkClass(
                    clazz = this::class,
                    expectedQualifiedName = null,
                    expectedSimpleName = null,
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$5\$1",
                    expectedInstance = this,
                    expectedNotInstance = Any()
            )
            checkClass(
                    clazz = this.innerKClass,
                    expectedQualifiedName = null,
                    expectedSimpleName = "Inner",
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$5\$1\$Inner",
                    expectedInstance = this.Inner(),
                    expectedNotInstance = Any()
            )
        }
    }.runCoroutine()

    // Anonymous object inside of suspend function.
    suspend fun suspendFunWithAnonymousObject() {
        // $1 goes for lambda created in `::suspendFunWithAnonymousObject.runCoroutine()` call.

        with(object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }) {
            checkClass(
                    clazz = this::class,
                    expectedQualifiedName = null,
                    expectedSimpleName = null,
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$suspendFunWithAnonymousObject\$2",
                    expectedInstance = this,
                    expectedNotInstance = Any()
            )
            checkClass(
                    clazz = this.innerKClass,
                    expectedQualifiedName = null,
                    expectedSimpleName = "Inner",
                    expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjects\$suspendFunWithAnonymousObject\$2\$Inner",
                    expectedInstance = this.Inner(),
                    expectedNotInstance = Any()
            )
        }
    }
    ::suspendFunWithAnonymousObject.runCoroutine()
}

private fun checkAnonymousObjectsAssignedToProperty(args: Array<String>) {
    // Anonymous object.
    val obj = object : Any() {
        val captured = args
        inner class Inner
        val innerKClass = Inner::class
    }
    checkClass(
            clazz = obj::class,
            expectedQualifiedName = null,
            expectedSimpleName = null,
            expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$1",
            expectedInstance = obj,
            expectedNotInstance = Any()
    )
    checkClass(
            clazz = obj.innerKClass,
            expectedQualifiedName = null,
            expectedSimpleName = "Inner",
            expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$1\$Inner",
            expectedInstance = obj.Inner(),
            expectedNotInstance = Any()
    )

    // Anonymous object inside of lambda.
    ;{
        val obj = object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }
        checkClass(
                clazz = obj::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$2\$1",
                expectedInstance = obj,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = obj.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$2\$1\$Inner",
                expectedInstance = obj.Inner(),
                expectedNotInstance = Any()
        )
    }.invoke()

    // Anonymous object inside of non-inlined lambda.
    runNonInlining {
        val obj = object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }
        checkClass(
                clazz = obj::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$3\$1",
                expectedInstance = obj,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = obj.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$3\$1\$Inner",
                expectedInstance = obj.Inner(),
                expectedNotInstance = Any()
        )
    }

    // Anonymous object inside of inlined lambda.
    run {
        val obj = object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }
        checkClass(
                clazz = obj::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$4",
                expectedInstance = obj,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = obj.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$4\$Inner",
                expectedInstance = obj.Inner(),
                expectedNotInstance = Any()
        )
    }

    // Anonymous object inside of suspend lambda.
    suspend {
        val obj = object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }
        checkClass(
                clazz = obj::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$5\$1",
                expectedInstance = obj,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = obj.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$5\$1\$Inner",
                expectedInstance = obj.Inner(),
                expectedNotInstance = Any()
        )
    }.runCoroutine()

    // Anonymous object inside of suspend function.
    suspend fun suspendFunWithAnonymousObject() {
        // $1 goes for lambda created in `::suspendFunWithAnonymousObject.runCoroutine()` call.

        val obj = object : Any() {
            val captured = args
            inner class Inner
            val innerKClass = Inner::class
        }
        checkClass(
                clazz = obj::class,
                expectedQualifiedName = null,
                expectedSimpleName = null,
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$suspendFunWithAnonymousObject\$2",
                expectedInstance = obj,
                expectedNotInstance = Any()
        )
        checkClass(
                clazz = obj.innerKClass,
                expectedQualifiedName = null,
                expectedSimpleName = "Inner",
                expectedToStringName = "class codegen.kclass.kclass0.checkAnonymousObjectsAssignedToProperty\$suspendFunWithAnonymousObject\$2\$Inner",
                expectedInstance = obj.Inner(),
                expectedNotInstance = Any()
        )
    }
    ::suspendFunWithAnonymousObject.runCoroutine()
}

private fun checkFunctionReferences() {
    // TODO add tests for lambdas and anonymous functions, see KT-47194
    fun foo() = Unit
    val ref = ::foo
    checkClass(
            clazz = ref::class,
            expectedQualifiedName = null,
            expectedSimpleName = null,
            expectedToStringName = "class codegen.kclass.kclass0.checkFunctionReferences\$1",
            expectedInstance = ref,
            expectedNotInstance = Any()
    )
}

private fun checkInterfaces() {
    checkClass(
            clazz = Comparable::class,
            expectedQualifiedName = "kotlin.Comparable",
            expectedSimpleName = "Comparable",
            expectedToStringName = "class kotlin.Comparable",
            expectedInstance = 42,
            expectedNotInstance = Any()
    )
    checkClass(
            clazz = Interface::class,
            expectedQualifiedName = "codegen.kclass.kclass0.Interface",
            expectedSimpleName = "Interface",
            expectedToStringName = "class codegen.kclass.kclass0.Interface",
            expectedInstance = object : Interface {},
            expectedNotInstance = Any()
    )
}

private fun checkEtc() {
    checkInstanceClass(Any(), Any::class)
    checkInstanceClass(42, Int::class)
    assertEquals(42::class, Int::class)

    class Local

    checkReifiedClass<Int>(Int::class)
    checkReifiedClass<Int?>(Int::class)
    checkReifiedClass2<Int>(Int::class)
    checkReifiedClass2<Int?>(Int::class)
    checkReifiedClass<Any>(Any::class)
    checkReifiedClass2<Any>(Any::class)
    checkReifiedClass2<Any?>(Any::class)
    checkReifiedClass<Local>(Local::class)
    checkReifiedClass2<Local>(Local::class)
    checkReifiedClass<TopLevel>(TopLevel::class)
    checkReifiedClass2<TopLevel>(TopLevel::class)
}


class TopLevel {
    class Nested
}
interface Interface

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

private fun checkInstanceClass(instance: Any, clazz: KClass<*>) {
    assertEquals(clazz, instance::class)
}

private inline fun <reified T> checkReifiedClass(expectedClass: KClass<*>) {
    assertEquals(expectedClass, T::class)
}

private inline fun <reified T> checkReifiedClass2(expectedClass: KClass<*>) {
    checkReifiedClass<T>(expectedClass)
    checkReifiedClass<T?>(expectedClass)
}

// Like `run` but without inlining.
private fun runNonInlining(block: () -> Unit) = block()

private fun <T> (suspend () -> T).runCoroutine() = startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
