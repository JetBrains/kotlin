/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal class Solver(
    private val inferenceContext: InferenceContext,
    private val printConstraints: Boolean,
    private val defaultStateProvider: DefaultStateProvider
) {
    private val printer = DebugPrinter(inferenceContext)

    private fun List<Constraint>.printDebugInfo(step: Int) =
        with(printer) {
            if (printConstraints) {
                println("Step $step:")
                for (constraint in this@printDebugInfo) {
                    println(constraint.asString())
                }
                println()
                println("type variables:")
                for (typeVariable in inferenceContext.typeVariables) {
                    if (typeVariable.state != State.UNUSED) {
                        println("${typeVariable.name} := ${typeVariable.state}")
                    }
                }
                println("---------------\n")
            }
        }

    fun solveConstraints(constraints: List<Constraint>) {
        val mutableConstraints = constraints.toMutableList()
        mutableConstraints.filterOutConstraintsWithUnusedState()
        var currentStep = ConstraintPriority.values().first()

        var i = 0
        do {
            var somethingChanged = false
            with(mutableConstraints) {
                printDebugInfo(i)
                somethingChanged = handleConstraintsWithNullableLowerBound(currentStep) || somethingChanged
                somethingChanged = handleConstraintsWithNotNullUpperBound(currentStep) || somethingChanged
                somethingChanged = handleEqualsConstraints(currentStep) || somethingChanged
                somethingChanged = substituteConstraints() || somethingChanged
                cleanConstraints()
            }


            if (!somethingChanged) {
                if (currentStep.ordinal < ConstraintPriority.values().lastIndex) {
                    currentStep = ConstraintPriority.values()[currentStep.ordinal + 1]
                    somethingChanged = true
                }
            }
            if (!somethingChanged) {
                val typeVariable =
                    mutableConstraints.getTypeVariableAsEqualsOrUpperBound()
                        ?: inferenceContext.typeVariables.firstOrNull { !it.isFixed }
                if (typeVariable != null) {
                    typeVariable.setStateIfNotFixed(defaultStateProvider.defaultStateFor(typeVariable))
                    somethingChanged = true
                }
            }
            i++
        } while (somethingChanged)
    }


    private fun MutableList<Constraint>.cleanConstraints() {
        removeIf { constraint ->
            constraint is SubtypeConstraint
                    && constraint.subtype is LiteralBound
                    && constraint.supertype is LiteralBound
        }
    }

    private fun MutableList<Constraint>.handleConstraintsWithNullableLowerBound(step: ConstraintPriority): Boolean {
        var somethingChanged = false
        val nullableConstraints = getConstraintsWithNullableLowerBound(step)
        if (nullableConstraints.isNotEmpty()) {
            this -= nullableConstraints
            for ((_, upperBound) in nullableConstraints) {
                if (upperBound is TypeVariableBound) {
                    somethingChanged = true
                    upperBound.typeVariable.setStateIfNotFixed(State.UPPER)
                }
            }
        }
        return somethingChanged
    }

    private fun MutableList<Constraint>.handleConstraintsWithNotNullUpperBound(step: ConstraintPriority): Boolean {
        var somethingChanged = false
        val nullableConstraints = getConstraintsWithNotNullUpperBound(step)
        if (nullableConstraints.isNotEmpty()) {
            this -= nullableConstraints
            for ((lowerBound, _) in nullableConstraints) {
                if (lowerBound is TypeVariableBound) {
                    somethingChanged = true
                    lowerBound.typeVariable.setStateIfNotFixed(State.LOWER)
                }
            }
        }
        return somethingChanged
    }

    private fun ConstraintBound.fixedState(): State? = when {
        this is LiteralBound -> state
        this is TypeVariableBound && typeVariable.isFixed -> typeVariable.state
        else -> null
    }

    private fun MutableList<Constraint>.handleEqualsConstraints(step: ConstraintPriority): Boolean {
        var somethingChanged = false
        val equalsConstraints = filterIsInstance<EqualsConstraint>().filter { it.priority <= step }
        if (equalsConstraints.isNotEmpty()) {
            for (constraint in equalsConstraints) {
                val (leftBound, rightBound) = constraint
                when {
                    leftBound is TypeVariableBound && rightBound.fixedState() != null -> {
                        this -= constraint
                        somethingChanged = true
                        leftBound.typeVariable.setStateIfNotFixed(rightBound.fixedState()!!)
                    }

                    rightBound is TypeVariableBound && leftBound.fixedState() != null -> {
                        this -= constraint
                        somethingChanged = true
                        rightBound.typeVariable.setStateIfNotFixed(leftBound.fixedState()!!)
                    }
                }
            }
        }
        return somethingChanged
    }


    private fun List<Constraint>.substituteConstraints(): Boolean {
        var somethingChanged = false
        for (constraint in this) {
            if (constraint is SubtypeConstraint) {
                val (lower, upper) = constraint
                if (lower is TypeVariableBound && lower.typeVariable.isFixed) {
                    somethingChanged = true
                    constraint.subtype = lower.typeVariable.state.constraintBound() ?: continue
                }
                if (upper is TypeVariableBound && upper.typeVariable.isFixed) {
                    somethingChanged = true
                    constraint.supertype = upper.typeVariable.state.constraintBound() ?: continue
                }
            }
        }
        return somethingChanged
    }

    private fun List<Constraint>.getConstraintsWithNullableLowerBound(priority: ConstraintPriority) =
        filterIsInstance<SubtypeConstraint>().filter { constraint ->
            constraint.priority <= priority
                    && constraint.subtype.safeAs<LiteralBound>()?.state == State.UPPER
        }

    private fun List<Constraint>.getConstraintsWithNotNullUpperBound(priority: ConstraintPriority) =
        filterIsInstance<SubtypeConstraint>().filter { constraint ->
            constraint.priority <= priority
                    && constraint.supertype.safeAs<LiteralBound>()?.state == State.LOWER
        }


    private fun List<Constraint>.getTypeVariableAsEqualsOrUpperBound(): TypeVariable? {
        for (constraint in this) {
            when (constraint) {
                is SubtypeConstraint -> {
                    constraint.supertype.safeAs<TypeVariableBound>()
                        ?.typeVariable
                        ?.takeIf { defaultStateProvider.defaultStateFor(it) == State.LOWER }
                        ?.let { return it }
                    constraint.subtype.safeAs<TypeVariableBound>()
                        ?.typeVariable
                        ?.takeIf { defaultStateProvider.defaultStateFor(it) == State.UPPER }
                        ?.let { return it }
                }
                is EqualsConstraint -> {
                    (constraint.left.safeAs<TypeVariableBound>()
                        ?: constraint.right.safeAs<TypeVariableBound>())
                        ?.let { return it.typeVariable }
                }
            }
        }
        return null
    }

    private fun MutableList<Constraint>.filterOutConstraintsWithUnusedState() {
        removeIf { constraint ->
            when (constraint) {
                is SubtypeConstraint -> constraint.subtype.isUnused || constraint.supertype.isUnused
                is EqualsConstraint -> constraint.left.isUnused || constraint.right.isUnused
            }
        }
    }
}

