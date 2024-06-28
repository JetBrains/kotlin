/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.let { it::class.js }, actual?.let { it::class.js })
}


public actual val TestPlatform.Companion.current: TestPlatform get() = TestPlatform.Js

// TODO: should be true at least in JS IR after implementing KT-24975
public actual val isFloat32RangeEnforced: Boolean = false

public actual val supportsOctalLiteralInRegex: Boolean get() = false

public actual val supportsEscapeAnyCharInRegex: Boolean get() = false

public actual val regexSplitUnicodeCodePointHandling: Boolean get() = true

public actual object BackReferenceHandling {
    actual val captureLargestValidIndex: Boolean get() = false

    actual val notYetDefinedGroup: HandlingOption = HandlingOption.IGNORE_BACK_REFERENCE_EXPRESSION
    actual val notYetDefinedNamedGroup: HandlingOption = HandlingOption.IGNORE_BACK_REFERENCE_EXPRESSION
    actual val enclosingGroup: HandlingOption = HandlingOption.IGNORE_BACK_REFERENCE_EXPRESSION
    actual val nonExistentGroup: HandlingOption = HandlingOption.THROW
    actual val nonExistentNamedGroup: HandlingOption = HandlingOption.THROW
    actual val groupZero: HandlingOption = HandlingOption.MATCH_NOTHING
}