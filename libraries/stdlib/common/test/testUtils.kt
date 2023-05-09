/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

public expect fun assertTypeEquals(expected: Any?, actual: Any?)

public expect val isFloat32RangeEnforced: Boolean

public expect val supportsOctalLiteralInRegex: Boolean

public expect val supportsEscapeAnyCharInRegex: Boolean

public expect val regexSplitUnicodeCodePointHandling: Boolean

public enum class HandlingOption {
    MATCH_NOTHING, THROW, IGNORE_BACK_REFERENCE_EXPRESSION
}

public expect object BackReferenceHandling {
    val captureLargestValidIndex: Boolean

    val notYetDefinedGroup: HandlingOption
    val notYetDefinedNamedGroup: HandlingOption
    val enclosingGroup: HandlingOption
    val nonExistentGroup: HandlingOption
    val nonExistentNamedGroup: HandlingOption
    val groupZero: HandlingOption
}
