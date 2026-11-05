/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw.bases

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class AnyNumber : Comparable<AnyNumber> {
    override operator fun compareTo(other: AnyNumber): Int

    operator fun inc(): AnyNumber
    operator fun dec(): AnyNumber
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect abstract class AnyNumberIterator : Iterator<AnyNumber> {
    override final fun next(): AnyNumber
    override abstract fun hasNext(): Boolean
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class AnyNumberRange : ClosedRange<AnyNumber> {
    override val start: AnyNumber
    override val endInclusive: AnyNumber
}

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect class SignedNumberRange : ClosedRange<AnyNumber>, Iterable<AnyNumber> {
    /** Include contents of [AnyNumberRange] */

    val step: AnyNumber

    override fun iterator(): AnyNumberIterator
}

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect class UnsignedNumberRange : ClosedRange<AnyNumber>, Iterable<AnyNumber> {
    /** Include contents of [AnyNumberRange] */

    override fun iterator(): Iterator<AnyNumber>
}
