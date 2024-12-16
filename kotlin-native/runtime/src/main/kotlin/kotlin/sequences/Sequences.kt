/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.sequences

import kotlin.comparisons.*

internal actual class ConstrainedOnceSequence<T> actual constructor(sequence: Sequence<T>) : Sequence<T> {
    private val sequenceRef = kotlin.concurrent.AtomicReference<Sequence<T>?>(sequence)

    override actual fun iterator(): Iterator<T> {
        val sequence = sequenceRef.getAndSet(null) ?: throw IllegalStateException("This sequence can be consumed only once.")
        return sequence.iterator()
    }
}
