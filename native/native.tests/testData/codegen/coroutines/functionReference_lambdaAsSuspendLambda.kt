/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun foo(block: (Continuation<Unit>) -> Any?) {
    block as (suspend () -> Unit)
}

fun box(): String {
    foo {}

    return "OK"
}