/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package lower.vararg

import kotlin.test.*

fun foo(vararg x: Any?) {}
fun bar() = foo()

@Test fun runTest() {
  bar()
}