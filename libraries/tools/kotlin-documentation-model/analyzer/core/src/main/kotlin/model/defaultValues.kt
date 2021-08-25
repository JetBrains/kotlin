package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class DefaultValue(val value: Expression): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<Documentable> = MergeStrategy.Remove // TODO pass a logger somehow and log this
    }

    override val key: ExtraProperty.Key<Documentable, *>
        get() = Companion
}

interface Expression
data class ComplexExpression(val value: String) : Expression
data class IntegerConstant(val value: Long) : Expression
data class StringConstant(val value: String) : Expression
data class DoubleConstant(val value: Double) : Expression
data class FloatConstant(val value: Float) : Expression
data class BooleanConstant(val value: Boolean) : Expression