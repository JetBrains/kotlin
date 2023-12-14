/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(s4: String, s5: String): String {
    return s4 + s5
}

fun bar(s1: String, s2: String, s3: String): String {
    return s1 + foo(s2, s3)
}

fun box(): String {
    assertEquals("Hello world", bar("Hello ", "wor", "ld"))
    return "OK"
}


