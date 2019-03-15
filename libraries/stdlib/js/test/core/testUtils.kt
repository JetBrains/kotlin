/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.let { it::class.js }, actual?.let { it::class.js })
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun String.removeLeadingPlusOnJava6(): String = this
