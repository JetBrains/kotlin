/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file: [Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "UNUSED_PARAMETER")]

package kotlin

class VArray<T>(val size: Int) {
    public operator fun get(index: Int): T = TODO()
    public operator fun set(index: Int, value: T): Unit = TODO()
}

