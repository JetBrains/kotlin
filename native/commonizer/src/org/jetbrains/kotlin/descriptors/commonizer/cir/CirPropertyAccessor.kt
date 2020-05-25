/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

interface CirPropertyAccessor {
    val annotations: List<CirAnnotation>
    val isDefault: Boolean
    val isExternal: Boolean
    val isInline: Boolean
}
