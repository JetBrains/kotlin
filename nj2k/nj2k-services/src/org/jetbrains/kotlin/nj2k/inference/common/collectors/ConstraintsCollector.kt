/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common.collectors

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculator
import org.jetbrains.kotlin.nj2k.inference.common.ConstraintBuilder
import org.jetbrains.kotlin.nj2k.inference.common.InferenceContext
import org.jetbrains.kotlin.psi.KtElement

abstract class ConstraintsCollector {
    abstract fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    )
}