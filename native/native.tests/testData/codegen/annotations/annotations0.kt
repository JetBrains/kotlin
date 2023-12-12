/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// FILE: 1.kt

import kotlin.test.*

@SerialInfo
annotation class Foo(val x: Int, val y: String)

fun box(): String {
    val foo = @Suppress("ANNOTATION_CLASS_CONSTRUCTOR_CALL") Foo(42, "OK")
    assertEquals(foo.x, 42)
    return foo.y
}

// FILE: 2.kt

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo
