/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.function.nothingN_returning_safe_call1

import kotlin.test.*

fun Any.nothing(): Nothing {
    while (true) {}
}

fun foo(obj: Any?): Nothing? = obj?.nothing()

fun main() {
    foo(null)
}
