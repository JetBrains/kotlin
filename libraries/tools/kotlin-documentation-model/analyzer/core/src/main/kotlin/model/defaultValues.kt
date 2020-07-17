package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class DefaultValue(val value: String): ExtraProperty<DParameter> {
    companion object : ExtraProperty.Key<DParameter, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<DParameter> = MergeStrategy.Remove // TODO pass a logger somehow and log this
    }

    override val key: ExtraProperty.Key<DParameter, *>
        get() = Companion
}