/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.kclass.kclass1

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    App(testQualified = true)
    return sb.toString()
}

// Taken from:
// https://github.com/SalomonBrys/kmffkn/blob/master/shared/main/kotlin/com/github/salomonbrys/kmffkn/app.kt

@DslMarker
annotation class MyDsl

@MyDsl
class DslMain {
    fun <T: Any> kClass(block: KClassDsl.() -> T): T = KClassDsl().block()
}

@MyDsl
class KClassDsl {
    inline fun <reified T: Any> of() = T::class
}

fun <T: Any> dsl(block: DslMain.() -> T): T = DslMain().block()

class TestClass

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
class App(testQualified: Boolean) {

    var type = dsl {
        kClass {
            //kClass {  } // This should error if uncommented because of `@DslMarker`.
            of<TestClass>()
        }
    }

    init {
        assert(type.simpleName == "TestClass")
        if (testQualified)
            assert(type.qualifiedName == "codegen.kclass.kclass1.TestClass") // This is not really necessary, but always better :).

        assert(String::class == String::class)
        assert(String::class != Int::class)

        assert(TestClass()::class == TestClass()::class)
        assert(TestClass()::class == TestClass::class)

        sb.append("OK")
    }
}
