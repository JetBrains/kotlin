/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.sequences

internal actual class ConstrainedOnceSequence<T> : Sequence<T> {
    actual constructor(sequence: Sequence<T>) { TODO("Wasm stdlib: ConstrainedOnceSequence") }

    actual override fun iterator(): Iterator<T> = TODO("Wasm stdlib: ConstrainedOnceSequence")
}
