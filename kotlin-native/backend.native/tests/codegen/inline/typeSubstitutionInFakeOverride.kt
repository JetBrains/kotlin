/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.typeSubstitutionInFakeOverride

import kotlin.test.*

abstract class A {
    inline fun <reified T : Any> baz(): String {
        return T::class.simpleName!!
    }
}

class B : A() {
    fun bar(): String {
        return baz<OK>()
    }
}

class OK

@Test fun runTest() {
    println(B().bar())
}