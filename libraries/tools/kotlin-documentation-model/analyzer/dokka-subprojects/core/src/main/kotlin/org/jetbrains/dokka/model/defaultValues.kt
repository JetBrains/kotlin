/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

public class DefaultValue(
    public val expression: SourceSetDependent<Expression>
): ExtraProperty<Documentable> {

    @Deprecated("Use `expression` property that depends on source set", ReplaceWith("this.expression.values.first()"))
    public val value: Expression
        get() = expression.values.first()

    public companion object : ExtraProperty.Key<Documentable, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<Documentable> =
            MergeStrategy.Replace(DefaultValue(left.expression + right.expression))

    }

    override val key: ExtraProperty.Key<Documentable, *>
        get() = Companion
}

public interface Expression
public data class ComplexExpression(val value: String) : Expression
public data class IntegerConstant(val value: Long) : Expression
public data class StringConstant(val value: String) : Expression
public data class DoubleConstant(val value: Double) : Expression
public data class FloatConstant(val value: Float) : Expression
public data class BooleanConstant(val value: Boolean) : Expression
