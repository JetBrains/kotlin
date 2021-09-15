/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTypeDistance.Companion.unreachable
import kotlin.math.absoluteValue

@JvmInline
value class CirTypeDistance(private val value: Int) : Comparable<CirTypeDistance> {

    val isReachable: Boolean get() = value != Int.MAX_VALUE

    val isNotReachable: Boolean get() = !isReachable

    val isNegative: Boolean get() = isReachable && value < 0

    val isPositive: Boolean get() = isReachable && value > 0

    val isZero: Boolean get() = value == 0

    val absoluteValue get() = CirTypeDistance(value.absoluteValue)

    operator fun plus(value: Int) = if (isReachable) CirTypeDistance(this.value + value) else this

    operator fun plus(value: CirTypeDistance) = when {
        this.isNotReachable -> this
        value.isNotReachable -> value
        else -> CirTypeDistance(this.value + value.value)
    }

    operator fun minus(value: Int) = if (isReachable) CirTypeDistance(this.value - value) else this

    operator fun minus(value: CirTypeDistance) = when {
        this.isNotReachable -> this
        value.isNotReachable -> value
        else -> CirTypeDistance(this.value - value.value)
    }

    operator fun inc() = this + 1

    operator fun dec() = this - 1

    override fun compareTo(other: CirTypeDistance): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return if (isNotReachable) "CirTypeDistance([unreachable])"
        else "CirTypeDistance($value)"
    }

    companion object {
        val unreachable: CirTypeDistance = CirTypeDistance(Int.MAX_VALUE)
    }
}

internal fun typeDistance(
    classifiers: CirKnownClassifiers,
    target: CommonizerTarget,
    from: CirClassOrTypeAliasType,
    to: CirEntityId
): CirTypeDistance {
    return typeDistance(classifiers, classifiers.classifierIndices.indexOf(target), from = from, to = to)
}

internal fun typeDistance(
    classifiers: CirKnownClassifiers,
    targetIndex: Int,
    from: CirClassOrTypeAliasType,
    to: CirEntityId
): CirTypeDistance {
    if (from.classifierId == to) return CirTypeDistance(0)
    val forwardDistance = forwardTypeDistance(from, to)
    if (forwardDistance.isReachable) return forwardDistance

    val backwardsDistance = backwardsTypeDistance(classifiers, targetIndex, from.classifierId, to)
    if (backwardsDistance.isReachable) return backwardsDistance

    val fromExpansion = from.expandedType()
    val distanceToExpansion = typeDistance(classifiers, targetIndex, from, fromExpansion.classifierId)
    return backwardsTypeDistance(classifiers, targetIndex, from.expandedType().classifierId, to) - distanceToExpansion
}

private fun forwardTypeDistance(from: CirClassOrTypeAliasType, to: CirEntityId): CirTypeDistance {
    if (from !is CirTypeAliasType) return unreachable

    var iteration = 0
    var underlyingType: CirClassOrTypeAliasType? = from.underlyingType

    while (true) {
        iteration++
        val capturedUnderlyingType = underlyingType ?: return unreachable
        if (capturedUnderlyingType.classifierId == to) return CirTypeDistance(iteration)
        underlyingType = (capturedUnderlyingType as? CirTypeAliasType)?.underlyingType
    }
}

private fun backwardsTypeDistance(
    classifiers: CirKnownClassifiers, targetIndex: Int, from: CirEntityId, to: CirEntityId
): CirTypeDistance {
    val resolvedDependencyClassifier =
        classifiers.commonDependencies.classifier(to) ?: classifiers.targetDependencies[targetIndex].classifier(to)
    if (resolvedDependencyClassifier != null) {
        return backwardsTypeDistance(classifiers, targetIndex, from, resolvedDependencyClassifier)
    }

    val resolvedClassifier = classifiers.classifierIndices[targetIndex].findClassifier(to) ?: return unreachable
    return backwardsTypeDistance(classifiers, targetIndex, from, resolvedClassifier)
}

private fun backwardsTypeDistance(
    classifiers: CirKnownClassifiers, targetIndex: Int, from: CirEntityId, to: CirProvided.Classifier
): CirTypeDistance {
    if (to !is CirProvided.TypeAlias) return unreachable
    if (to.underlyingType.classifierId == from) return CirTypeDistance(-1)
    return backwardsTypeDistance(classifiers, targetIndex, from, to.underlyingType.classifierId) - 1
}

private fun backwardsTypeDistance(
    classifiers: CirKnownClassifiers, targetIndex: Int, from: CirEntityId, to: CirClassifier
): CirTypeDistance {
    if (to !is CirTypeAlias) return unreachable
    if (to.underlyingType.classifierId == from) return CirTypeDistance(-1)
    return backwardsTypeDistance(classifiers, targetIndex, from, to.underlyingType.classifierId) - 1
}
