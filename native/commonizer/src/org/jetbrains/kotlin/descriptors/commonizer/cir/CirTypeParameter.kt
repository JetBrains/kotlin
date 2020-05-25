/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

interface CirTypeParameter {
    val annotations: List<CirAnnotation>
    val name: Name
    val isReified: Boolean
    val variance: Variance
    val upperBounds: List<CirType>
}
