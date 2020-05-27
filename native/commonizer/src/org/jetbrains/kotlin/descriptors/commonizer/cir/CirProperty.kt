/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.resolve.constants.ConstantValue

interface CirProperty : CirFunctionOrProperty, CirLiftedUpDeclaration {
    val isVar: Boolean
    val isLateInit: Boolean
    val isConst: Boolean
    val isDelegate: Boolean
    val getter: CirPropertyGetter?
    val setter: CirPropertySetter?
    val backingFieldAnnotations: List<CirAnnotation>? // null assumes no backing field
    val delegateFieldAnnotations: List<CirAnnotation>? // null assumes no backing field
    val compileTimeInitializer: ConstantValue<*>?
}
