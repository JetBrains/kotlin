/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.*
import kotlin.reflect.qualifiedOrSimpleName

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.let { it::class }, actual?.let { it::class })
}

public actual val TestPlatform.Companion.current: TestPlatform get() = TestPlatform.Wasm

// TODO: See KT-24975
public actual val isFloat32RangeEnforced: Boolean = false

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