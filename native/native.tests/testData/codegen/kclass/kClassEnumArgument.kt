/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.KClass

enum class E(val arg: KClass<*>?) {
    A(null as KClass<*>?),
    B(String::class);
}

fun box(): String {
    assertEquals("String", E.B.arg?.simpleName)

    return "OK"
}