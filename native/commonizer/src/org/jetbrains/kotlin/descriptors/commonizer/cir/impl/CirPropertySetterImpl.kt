/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter

data class CirPropertySetterImpl(
    override val annotations: List<CirAnnotation>,
    override val parameterAnnotations: List<CirAnnotation>,
    override val visibility: Visibility,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : CirPropertySetter
