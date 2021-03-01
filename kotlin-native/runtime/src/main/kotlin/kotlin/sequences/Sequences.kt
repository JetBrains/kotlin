/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.sequences

import kotlin.comparisons.*
import kotlin.native.internal.FixmeConcurrency

@FixmeConcurrency
internal actual class ConstrainedOnceSequence<T> actual constructor(sequence: Sequence<T>) : Sequence<T> {
    // TODO: not MT friendly.
    private var sequenceRef : Sequence<T>? = sequence

    override actual fun iterator(): Iterator<T> {
        val sequence = sequenceRef
        if (sequence == null) throw IllegalStateException("This sequence can be consumed only once.")
        sequenceRef = null
        return sequence.iterator()
    }
}
