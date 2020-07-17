package org.jetbrains.dokka.model.properties

interface ExtraProperty<in C : Any> {
    interface Key<in C : Any, T : Any> {
        fun mergeStrategyFor(left: T, right: T): MergeStrategy<C> = MergeStrategy.Fail {
            throw NotImplementedError("Property merging for $this is not implemented")
        }
    }

    val key: Key<C, *>
}

interface CalculatedProperty<in C : Any, T : Any> : ExtraProperty.Key<C, T> {
    fun calculate(subject: C): T
}

sealed class MergeStrategy<in C> {
    class Replace<in C : Any>(val newProperty: ExtraProperty<C>) : MergeStrategy<C>()
    object Remove : MergeStrategy<Any>()
    class Full<C : Any>(val merger: (preMerged: C, left: C, right: C) -> C) : MergeStrategy<C>()
    class Fail(val error: () -> Nothing) : MergeStrategy<Any>()
}
