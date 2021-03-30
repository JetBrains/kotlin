/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults7

import kotlin.test.*

/**
 * Test fails with code generation:
 * Call parameter type does not match function signature!
 * %5 = invoke %struct.ObjHeader* @"kfun:kotlin.native.internal.boxInt(kotlin.Int)"(i32 1, %struct.ObjHeader** %4)
 * to label %label_ unwind label %cleanup_landingpad
 * i32  invoke void @"kfun:foo$default(kotlin.Int;kotlin.Int;kotlin.Int)"(%struct.ObjHeader* %5, i32 0, i32 2)
 * to label %label_1 unwind label %cleanup_landingpad
 */

fun <T> foo(a : T, b : Int = 42){
    println(b)
}

@Test fun runTest() {
    foo(1)
}