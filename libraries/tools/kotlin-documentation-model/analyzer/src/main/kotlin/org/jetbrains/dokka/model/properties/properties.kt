/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.properties

public interface ExtraProperty<in C : Any> {
    public interface Key<in C : Any, T : Any> {
        public fun mergeStrategyFor(left: T, right: T): MergeStrategy<C> = MergeStrategy.Fail {
            throw NotImplementedError("Property merging for $this is not implemented")
        }
    }

    public val key: Key<C, *>
}

public interface CalculatedProperty<in C : Any, T : Any> : ExtraProperty.Key<C, T> {
    public fun calculate(subject: C): T
}

public sealed class MergeStrategy<in C> {

    public class Replace<in C : Any>(
        public val newProperty: ExtraProperty<C>
    ) : MergeStrategy<C>()

    public object Remove : MergeStrategy<Any>()

    public class Full<C : Any>(
        public val merger: (preMerged: C, left: C, right: C) -> C
    ) : MergeStrategy<C>()

    public class Fail(
        public val error: () -> Nothing
    ) : MergeStrategy<Any>()
}
