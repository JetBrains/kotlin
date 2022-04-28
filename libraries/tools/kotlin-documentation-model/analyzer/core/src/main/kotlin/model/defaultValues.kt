package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class DefaultValue(val expression: SourceSetDependent<Expression>): ExtraProperty<Documentable> {

    @Deprecated("Use `expression` property that depends on source set", ReplaceWith("this.expression.values.first()"))
    val value: Expression
        get() = expression.values.first()
    companion object : ExtraProperty.Key<Documentable, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<Documentable> =
            MergeStrategy.Replace(DefaultValue(left.expression + right.expression))

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