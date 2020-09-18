package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class DefaultValue(val value: String): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<Documentable> = MergeStrategy.Remove // TODO pass a logger somehow and log this
    }

    override val key: ExtraProperty.Key<Documentable, *>
        get() = Companion
}
