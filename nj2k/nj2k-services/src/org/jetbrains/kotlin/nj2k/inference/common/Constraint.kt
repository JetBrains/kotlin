/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

enum class ConstraintPriority {
    /* order of entries here used when solving system of constraints */
    SUPER_DECLARATION,
    INITIALIZER,
    RETURN,
    ASSIGNMENT,
    PARAMETER,
    RECEIVER_PARAMETER,
    COMPARE_WITH_NULL,
    USE_AS_RECEIVER,
}

sealed class Constraint {
    abstract val priority: ConstraintPriority
}

class SubtypeConstraint(
    var subtype: ConstraintBound,
    var supertype: ConstraintBound,
    override val priority: ConstraintPriority
) : Constraint() {
    operator fun component1() = subtype
    operator fun component2() = supertype
}

class EqualsConstraint(
    var left: ConstraintBound,
    var right: ConstraintBound,
    override val priority: ConstraintPriority
) : Constraint() {
    operator fun component1() = left
    operator fun component2() = right
}

fun Constraint.copy() = when (this) {
    is SubtypeConstraint -> SubtypeConstraint(subtype, supertype, priority)
    is EqualsConstraint -> EqualsConstraint(left, right, priority)
}

sealed class ConstraintBound
class TypeVariableBound(val typeVariable: TypeVariable) : ConstraintBound()
class LiteralBound private constructor(val state: State) : ConstraintBound() {
    companion object {
        val UPPER = LiteralBound(State.UPPER)
        val LOWER = LiteralBound(State.LOWER)
        val UNKNOWN = LiteralBound(State.UNKNOWN)
    }
}

val TypeVariable.constraintBound: TypeVariableBound
    get() = TypeVariableBound(this)

val State.constraintBound: LiteralBound
    get() = when (this) {
        State.LOWER -> LiteralBound.LOWER
        State.UPPER -> LiteralBound.UPPER
        State.UNKNOWN -> LiteralBound.UNKNOWN
    }

val BoundTypeLabel.constraintBound: ConstraintBound?
    get() = when (this) {
        is TypeVariableLabel -> typeVariable.constraintBound
        is TypeParameterLabel -> null
        is GenericLabel -> null
        StarProjectionLabel -> null
        NullLiteralLabel -> LiteralBound.UPPER
        LiteralLabel -> LiteralBound.LOWER
    }

val BoundType.constraintBound: ConstraintBound?
    get() = when (this) {
        is BoundTypeImpl -> label.constraintBound
        is WithForcedStateBoundType -> forcedState.constraintBound
    }