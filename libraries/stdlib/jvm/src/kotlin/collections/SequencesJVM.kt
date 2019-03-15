/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")

package kotlin.sequences

/**
 * Creates a sequence that returns all values from this enumeration. The sequence is constrained to be iterated only once.
 * @sample samples.collections.Sequences.Building.sequenceFromEnumeration
 */
@kotlin.internal.InlineOnly
public inline fun <T> java.util.Enumeration<T>.asSequence(): Sequence<T> = this.iterator().asSequence()


internal actual class ConstrainedOnceSequence<T> actual constructor(sequence: Sequence<T>) : Sequence<T> {
    private val sequenceRef = java.util.concurrent.atomic.AtomicReference(sequence)

    actual override fun iterator(): Iterator<T> {
        val sequence = sequenceRef.getAndSet(null) ?: throw IllegalStateException("This sequence can be consumed only once.")
        return sequence.iterator()
    }
}
