/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.convert
import kotlin.test.assertEquals

internal fun assertPrints(expression: Any?, expectedOutput: String, message: String? = null) =
    assertEquals(expectedOutput, expression.toString(), message)

internal fun pli(int: Int): PlatformInt = int.convert()
internal fun plui(uInt: UInt): PlatformUInt = uInt.convert()
internal fun pli(long: Long): PlatformInt = long.convert()
internal fun plui(uLong: ULong): PlatformUInt = uLong.convert()
