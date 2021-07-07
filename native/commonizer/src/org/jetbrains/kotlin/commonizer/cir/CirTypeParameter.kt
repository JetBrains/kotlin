/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.types.Variance

data class CirTypeParameter(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    val isReified: Boolean,
    val variance: Variance,
    val upperBounds: List<CirType>
) : CirHasAnnotations, CirHasName
