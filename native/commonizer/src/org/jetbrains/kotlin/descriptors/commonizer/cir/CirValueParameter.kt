/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.name.Name

interface CirValueParameter {
    val annotations: List<CirAnnotation>
    val name: Name
    val returnType: CirType
    val varargElementType: CirType?
    val declaresDefaultValue: Boolean
    val isCrossinline: Boolean
    val isNoinline: Boolean
}
