/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kt49034.bar
import cnames.structs.JSContext
import kotlinx.cinterop.CPointer

fun baz(s: CPointer<JSContext>) {
    println(s)
}

fun main() {
    baz(bar()!!)
}