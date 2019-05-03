/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal class Solver(
    private val analysisContext: AnalysisContext,
    private val printConstraints: Boolean
) {
    private val printer = Printer(analysisContext)

    private fun List<Constraint>.printDebugInfo(step: Int) =
        with(printer) {
            if (printConstraints) {
                println("Step $step:")
                println(listConstrains())
                println()
                println("type variables:")
                for (typeVariable in analysisContext.typeElementToTypeVariable.values) {
                    println("${typeVariable.name} := ${typeVariable.nullability}")
                }
                println("---------------\n")
            }
        }

    fun solveConstraints(constraints: List<Constraint>) {
        val mutableConstraints = constraints.toMutableList()
        var currentStep = ConstraintCameFrom.values().first()

        var i = 0
        do {
            var somethingChanged = false
            with(mutableConstraints) {
                printDebugInfo(i)
                somethingChanged = handleConstraintsWithNullableLowerBound(currentStep) || somethingChanged
                somethingChanged = handleConstraintsWithNotNullUpperBound(currentStep) || somethingChanged
                somethingChanged = handleEqualConstraints(currentStep) || somethingChanged
                somethingChanged = substituteConstraints() || somethingChanged
                cleanConstraints()
            }


            if (!somethingChanged) {
                if (currentStep.ordinal < ConstraintCameFrom.values().lastIndex) {
                    currentStep = ConstraintCameFrom.values()[currentStep.ordinal + 1]
                    somethingChanged = true
                }
            }
            if (!somethingChanged) {
                val typeVariable = mutableConstraints.getTypeVariableAsEqualsOrUpperBound()
                if (typeVariable != null) {
                    typeVariable.setNullabilityIfNotFixed(Nullability.NOT_NULL)
                    somethingChanged = true
                }
            }
            i++
        } while (somethingChanged)
    }


    private fun MutableList<Constraint>.cleanConstraints() {
        val newConstraints =
            distinct()
                .filterNot { constraint ->
                    constraint is SubtypeConstraint
                            && constraint.lowerBound is LiteralBound
                            && constraint.upperBound is LiteralBound
                }

        if (newConstraints.size < size) {
            clear()
            addAll(newConstraints)
        }
    }

    private fun MutableList<Constraint>.handleConstraintsWithNullableLowerBound(step: ConstraintCameFrom): Boolean {
        var somethingChanged = false
        val nullableConstraints = getConstraintsWithNullableLowerBound(step)
        if (nullableConstraints.isNotEmpty()) {
            this -= nullableConstraints
            for ((_, upperBound) in nullableConstraints) {
                if (upperBound is TypeVariableBound) {
                    somethingChanged = true
                    upperBound.typeVariable.setNullabilityIfNotFixed(Nullability.NULLABLE)
                }
            }
        }
        return somethingChanged
    }

    private fun MutableList<Constraint>.handleConstraintsWithNotNullUpperBound(step: ConstraintCameFrom): Boolean {
        var somethingChanged = false
        val nullableConstraints = getConstraintsWithNotNullUpperBound(step)
        if (nullableConstraints.isNotEmpty()) {
            this -= nullableConstraints
            for ((lowerBound, _) in nullableConstraints) {
                if (lowerBound is TypeVariableBound) {
                    somethingChanged = true
                    lowerBound.typeVariable.setNullabilityIfNotFixed(Nullability.NOT_NULL)
                }
            }
        }
        return somethingChanged
    }

    private fun ConstraintBound.fixedNullability(): Nullability? =
        when {
            this is LiteralBound -> nullability
            this is TypeVariableBound && typeVariable.isFixed -> typeVariable.nullability
            else -> null
        }

    private fun MutableList<Constraint>.handleEqualConstraints(step: ConstraintCameFrom): Boolean {
        var somethingChanged = false
        val equalsConstraints = filterIsInstance<EqualConstraint>().filter { it.cameFrom <= step }
        if (equalsConstraints.isNotEmpty()) {
            for (constraint in equalsConstraints) {
                val (leftBound, rightBound) = constraint
                when {
                    leftBound is TypeVariableBound && rightBound.fixedNullability() != null -> {
                        this -= constraint
                        somethingChanged = true
                        leftBound.typeVariable.setNullabilityIfNotFixed(rightBound.fixedNullability()!!)
                    }

                    rightBound is TypeVariableBound && leftBound.fixedNullability() != null -> {
                        this -= constraint
                        somethingChanged = true
                        rightBound.typeVariable.setNullabilityIfNotFixed(leftBound.fixedNullability()!!)
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
                    constraint.lowerBound = LiteralBound(lower.typeVariable.nullability)
                }
                if (upper is TypeVariableBound && upper.typeVariable.isFixed) {
                    somethingChanged = true
                    constraint.upperBound = LiteralBound(upper.typeVariable.nullability)
                }
            }
        }
        return somethingChanged
    }

    private fun List<Constraint>.getConstraintsWithNullableLowerBound(cameFrom: ConstraintCameFrom) =
        filterIsInstance<SubtypeConstraint>().filter { constraint ->
            constraint.cameFrom <= cameFrom
                    && constraint.lowerBound.safeAs<LiteralBound>()?.nullability == Nullability.NULLABLE
        }

    private fun List<Constraint>.getConstraintsWithNotNullUpperBound(cameFrom: ConstraintCameFrom) =
        filterIsInstance<SubtypeConstraint>().filter { constraint ->
            constraint.cameFrom <= cameFrom
                    && constraint.upperBound.safeAs<LiteralBound>()?.nullability == Nullability.NOT_NULL
        }


    private fun List<Constraint>.getTypeVariableAsEqualsOrUpperBound(): TypeVariable? =
        asSequence().filterIsInstance<SubtypeConstraint>()
            .map { it.upperBound }
            .firstIsInstanceOrNull<TypeVariableBound>()
            ?.typeVariable
            ?: asSequence().filterIsInstance<EqualConstraint>()
                .flatMap { sequenceOf(it.leftBound, it.rightBound) }
                .firstIsInstanceOrNull<TypeVariableBound>()
                ?.typeVariable

}

