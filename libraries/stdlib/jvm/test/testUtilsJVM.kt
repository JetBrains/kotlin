/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import java.util.*
import kotlin.test.assertEquals

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.javaClass, actual?.javaClass)
}

public actual val TestPlatform.Companion.current: TestPlatform get() = TestPlatform.Jvm

@Suppress("HasPlatformType", "UNCHECKED_CAST")
public fun <T> platformNull() = Collections.singletonList(null as T).first()

public actual val isFloat32RangeEnforced: Boolean = true

public actual val supportsOctalLiteralInRegex: Boolean get() = true

public actual val supportsEscapeAnyCharInRegex: Boolean get() = true

public actual val regexSplitUnicodeCodePointHandling: Boolean get() = false

public actual object BackReferenceHandling {
    actual val captureLargestValidIndex: Boolean get() = true

    actual val notYetDefinedGroup: HandlingOption = HandlingOption.MATCH_NOTHING
    actual val notYetDefinedNamedGroup: HandlingOption = HandlingOption.THROW
    actual val enclosingGroup: HandlingOption = HandlingOption.MATCH_NOTHING
    actual val nonExistentGroup: HandlingOption = HandlingOption.MATCH_NOTHING
    actual val nonExistentNamedGroup: HandlingOption = HandlingOption.THROW
    actual val groupZero: HandlingOption = HandlingOption.THROW
}
