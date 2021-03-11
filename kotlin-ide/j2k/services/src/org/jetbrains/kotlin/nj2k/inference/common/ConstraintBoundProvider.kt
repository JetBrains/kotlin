/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

interface ConstraintBoundProvider {
    fun TypeVariable.constraintBound(): TypeVariableBound
    fun BoundType.constraintBound(): ConstraintBound?
    fun BoundTypeLabel.constraintBound(): ConstraintBound?
}

abstract class ConstraintBoundProviderImpl : ConstraintBoundProvider {
    final override fun TypeVariable.constraintBound(): TypeVariableBound =
        TypeVariableBound(this)

    final override fun BoundType.constraintBound(): ConstraintBound? = when (this) {
        is BoundTypeImpl -> label.constraintBound()
        is WithForcedStateBoundType -> forcedState.constraintBound()
    }
}