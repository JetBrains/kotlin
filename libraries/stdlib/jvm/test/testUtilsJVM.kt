/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.assertEquals

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.javaClass, actual?.javaClass)
}

private val isJava6 = System.getProperty("java.version").startsWith("1.6.")

internal actual fun String.removeLeadingPlusOnJava6(): String =
    if (isJava6) removePrefix("+") else this

