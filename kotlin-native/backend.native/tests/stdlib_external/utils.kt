/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    if (expected != null && actual != null) {
        assertTrue(expected::class.isInstance(actual) || actual::class.isInstance(expected),
                "Expected: $expected,  Actual: $actual")
    } else {
        assertTrue(expected == null && actual == null)
    }
}

public actual val TestPlatform.Companion.current: TestPlatform get() = TestPlatform.Native

public actual val isFloat32RangeEnforced: Boolean get() = true

public actual val supportsOctalLiteralInRegex: Boolean get() = true

public actual val supportsEscapeAnyCharInRegex: Boolean get() = true

public actual val regexSplitUnicodeCodePointHandling: Boolean get() = true

public actual object BackReferenceHandling {
    actual val captureLargestValidIndex: Boolean get() = true

    actual val notYetDefinedGroup: HandlingOption = HandlingOption.THROW
    actual val notYetDefinedNamedGroup: HandlingOption = HandlingOption.THROW
    actual val enclosingGroup: HandlingOption = HandlingOption.MATCH_NOTHING
    actual val nonExistentGroup: HandlingOption = HandlingOption.THROW
    actual val nonExistentNamedGroup: HandlingOption = HandlingOption.THROW
    actual val groupZero: HandlingOption = HandlingOption.THROW
}