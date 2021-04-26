/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.function.nothingN_returning_safe_call2

import kotlin.test.*

fun Any.nothing(): Nothing {
    while (true) {}
}

fun foo() {
    val block: (Any?) -> Nothing? = {
        it?.nothing()
    }
    block(null)
}

fun main() {
    foo()
}
