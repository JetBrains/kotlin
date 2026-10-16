/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

internal class DefaultValue(
    val expression: SourceSetDependent<Expression>
): ExtraProperty<Documentable> {

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

internal interface Expression
internal data class ComplexExpression(val value: String) : Expression
internal data class IntegerConstant(val value: Long) : Expression
internal data class StringConstant(val value: String) : Expression
internal data class DoubleConstant(val value: Double) : Expression
internal data class FloatConstant(val value: Float) : Expression
internal data class BooleanConstant(val value: Boolean) : Expression
