package kotlin.sequences

internal header class ConstrainedOnceSequence<T> : Sequence<T> {
    constructor(sequence: Sequence<T>)

    override fun iterator(): Iterator<T>
}
