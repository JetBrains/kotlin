/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

data class CirTypeParameterImpl(
    override val annotations: List<CirAnnotation>,
    override val name: Name,
    override val isReified: Boolean,
    override val variance: Variance,
    override val upperBounds: List<CirType>
) : CirTypeParameter
