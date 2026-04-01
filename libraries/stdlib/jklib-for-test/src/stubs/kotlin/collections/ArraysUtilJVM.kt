/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.collections

import java.util.Arrays

// Stubbed to provide a minimal dependency-free implementation of Arrays.asList for the compiler backend
// without bringing in the full standard library interoperability layer.
// jvm-minimal-for-test includes the real source for this.
internal object ArraysUtilJVM {
    @JvmStatic
    fun <T> asList(array: Array<T>): MutableList<T> {
        return Arrays.asList(*array)
    }
}
