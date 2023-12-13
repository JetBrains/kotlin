/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.sum_silly

import kotlin.test.*

// FIXME: has no checks

fun sum(a:Int, b:Int):Int {
 var c:Int
 c = a + b
 return c
}