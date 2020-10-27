/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")
package codegen.inlineClass.customEquals

import kotlin.test.*

private inline class Z(val data: Int) {
    override fun equals(other: Any?) = other is Z && data % 256 == other.data % 256
}

@Test fun runTest() {
    assertTrue(Z(0) == Z(256))
}
