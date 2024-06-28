/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

data class CirProperty(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val containingClass: CirContainingClass?,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    val isVar: Boolean,
    val isLateInit: Boolean,
    val isConst: Boolean,
    val isDelegate: Boolean,
    val getter: CirPropertyGetter?,
    val setter: CirPropertySetter?,
    val backingFieldAnnotations: List<CirAnnotation>,
    val delegateFieldAnnotations: List<CirAnnotation>,
    val compileTimeInitializer: CirConstantValue
) : CirFunctionOrProperty, CirLiftedUpDeclaration {
    // const property in "common" fragment is already lifted up
    override val isLiftedUp get() = isConst

    override fun withContainingClass(containingClass: CirContainingClass): CirProperty {
        return copy(containingClass = containingClass)
    }
}
