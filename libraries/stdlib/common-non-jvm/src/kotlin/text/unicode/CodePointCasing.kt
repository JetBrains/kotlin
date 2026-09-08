/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

@ExperimentalUnicodeApi
public actual fun CodePoint.titlecaseCodePoint(): CodePoint {
    if (isBasic) {
        return code.toChar().titlecaseChar().toCodePoint()
    }
    // no different titlecase mapping outside BMP
    // no one-to-many uppercase expansions outside BMP
    return uppercaseCodePoint()
}
