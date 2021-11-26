/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    TODO("Implement class references")
    //assertEquals(expected?.let { it::class.js }, actual?.let { it::class.js })
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun String.removeLeadingPlusOnJava6(): String = this

internal actual inline fun testOnNonJvm6And7(f: () -> Unit) {
    f()
}


public actual fun testOnJvm(action: () -> Unit) { }
public actual fun testOnJs(action: () -> Unit) { }

// TODO: See KT-24975
public actual val isFloat32RangeEnforced: Boolean = false

// TODO: We need to implement this on wasm
actual val supportsSuppressedExceptions: Boolean get() = false

// TODO: implement named group reference in replacement expression
public actual val supportsNamedCapturingGroup: Boolean get() = false

public actual val regexSplitUnicodeCodePointHandling: Boolean get() = true