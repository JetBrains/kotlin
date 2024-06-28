/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.sequences.*

suspend fun SequenceScope<Int>.fill() {
    yield(1)
    yield(2)
}

fun getFillFunction() = SequenceScope<Int>::fill

fun callback(block: suspend SequenceScope<Int>.() -> Unit) : List<Int> {
    return sequence {
        block()
    }.toList()
}