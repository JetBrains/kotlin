/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

interface CirProperty : CirFunctionOrProperty, CirLiftedUpDeclaration {
    val isExternal: Boolean
    val isVar: Boolean
    val isLateInit: Boolean
    val isConst: Boolean
    val isDelegate: Boolean
    val getter: CirPropertyGetter?
    val setter: CirPropertySetter?
    val backingFieldAnnotations: List<CirAnnotation>
    val delegateFieldAnnotations: List<CirAnnotation>
    val compileTimeInitializer: CirConstantValue

    override fun withContainingClass(containingClass: CirContainingClass): CirProperty

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            name: CirName,
            typeParameters: List<CirTypeParameter>,
            visibility: Visibility,
            modality: Modality,
            containingClass: CirContainingClass?,
            isExternal: Boolean,
            extensionReceiver: CirExtensionReceiver?,
            returnType: CirType,
            kind: CallableMemberDescriptor.Kind,
            isVar: Boolean,
            isLateInit: Boolean,
            isConst: Boolean,
            isDelegate: Boolean,
            getter: CirPropertyGetter?,
            setter: CirPropertySetter?,
            backingFieldAnnotations: List<CirAnnotation>,
            delegateFieldAnnotations: List<CirAnnotation>,
            compileTimeInitializer: CirConstantValue
        ): CirProperty = CirPropertyImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            containingClass = containingClass,
            isExternal = isExternal,
            extensionReceiver = extensionReceiver,
            returnType = returnType,
            kind = kind,
            isVar = isVar,
            isLateInit = isLateInit,
            isConst = isConst,
            isDelegate = isDelegate,
            getter = getter,
            setter = setter,
            backingFieldAnnotations = backingFieldAnnotations,
            delegateFieldAnnotations = delegateFieldAnnotations,
            compileTimeInitializer = compileTimeInitializer
        )
    }
}

data class CirPropertyImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val containingClass: CirContainingClass?,
    override val isExternal: Boolean,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    override val isVar: Boolean,
    override val isLateInit: Boolean,
    override val isConst: Boolean,
    override val isDelegate: Boolean,
    override val getter: CirPropertyGetter?,
    override val setter: CirPropertySetter?,
    override val backingFieldAnnotations: List<CirAnnotation>,
    override val delegateFieldAnnotations: List<CirAnnotation>,
    override val compileTimeInitializer: CirConstantValue
) : CirProperty {
    // const property in "common" fragment is already lifted up
    override val isLiftedUp get() = isConst

    override fun withContainingClass(containingClass: CirContainingClass): CirProperty {
        return copy(containingClass = containingClass)
    }
}
