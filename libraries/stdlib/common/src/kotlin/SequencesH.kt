package kotlin.sequences

internal expect class ConstrainedOnceSequence<T> : Sequence<T> {
    constructor(sequence: Sequence<T>)

    override fun iterator(): Iterator<T>
}
