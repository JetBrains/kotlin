/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.Modality as KmModality
import kotlinx.metadata.ClassKind as KmClassKind
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities

internal fun KmFunction.modifiersFrom(cf: CirFunction, isExpect: Boolean) {
    hasAnnotations = cf.hasAnnotations
    visibility = cf.kmVisibility
    modality = cf.kmModality
    kind = cf.kind.kmMemberKind
    hasNonStableParameterNames = !cf.hasStableParameterNames
    this.isExpect = isExpect
    isOperator = cf.modifiers.isOperator
    isInfix = cf.modifiers.isInfix
    isInline = cf.modifiers.isInline
    isSuspend = cf.modifiers.isSuspend
}

internal fun KmProperty.modifiersFrom(cp: CirProperty, isExpect: Boolean) {
    hasAnnotations = cp.hasAnnotations
    visibility = cp.kmVisibility
    modality = cp.kmModality
    kind = cp.kind.kmMemberKind
    isDelegated = cp.isDelegate
    this.isExpect = isExpect
    isVar = cp.isVar
    isConst = cp.isConst
    hasConstant = cp.compileTimeInitializer.takeIf { it !is CirConstantValue.NullValue } != null
    isLateinit = cp.isLateInit
}

internal fun KmPropertyAccessorAttributes.modifiersFrom(
    cp: CirPropertyAccessor, visibilityHolder: CirHasVisibility,
    modalityHolder: CirHasModality,
) {
    hasAnnotations = cp.hasAnnotations
    visibility = visibilityHolder.kmVisibility
    modality = modalityHolder.kmModality
    isNotDefault = !cp.isDefault
    isInline = cp.isInline
}

internal fun KmConstructor.modifiersFrom(cc: CirClassConstructor) {
    hasAnnotations = cc.hasAnnotations
    visibility = cc.kmVisibility
    isSecondary = !cc.isPrimary
    hasNonStableParameterNames = !cc.hasStableParameterNames
}

internal fun CirType.applyTypeFlagsTo(type: KmType) {
    type.isNullable = when (this) {
        is CirSimpleType -> isMarkedNullable
        is CirFlexibleType -> lowerBound.isMarkedNullable
    }
    //Flag.Type.IS_SUSPEND.takeIf { false }
}

internal fun KmValueParameter.modifiersFrom(cv: CirValueParameter) {
    hasAnnotations = cv.hasAnnotations
    declaresDefaultValue = cv.declaresDefaultValue
    isCrossinline = cv.isCrossinline
    isNoinline = cv.isNoinline
}

internal fun KmClass.modifiersFrom(cc: CirClass, isExpect: Boolean) {
    hasAnnotations = cc.hasAnnotations
    visibility = cc.kmVisibility
    modality = cc.kmModality
    kind = cc.kmClassKind
    isInner = cc.isInner
    isData = cc.isData
    this.isExpect = isExpect
    isValue = cc.isValue
    hasEnumEntries = cc.hasEnumEntries
    //Flag.Class.IS_FUN.takeIf { false }
}

internal fun KmTypeAlias.modifiersFrom(ct: CirTypeAlias) {
    hasAnnotations = ct.hasAnnotations
    visibility = ct.kmVisibility
}

private inline val CirHasAnnotations.hasAnnotations: Boolean
    get() = annotations.isNotEmpty()

// Since 1.4.30 a special @JvmInline annotation is generated to distinguish JVM-inline from value classes.
// This has an effect on class serialization: Every class with isValue == true automatically gets HAS_ANNOTATIONS flag.
private inline val CirClass.hasAnnotations: Boolean
    get() = annotations.isNotEmpty() || isValue

private inline val CirProperty.hasAnnotations: Boolean
    get() = annotations.isNotEmpty() || backingFieldAnnotations.isNotEmpty() || delegateFieldAnnotations.isNotEmpty()

private inline val CirHasVisibility.kmVisibility: Visibility
    get() = when (visibility) {
        Visibilities.Public -> Visibility.PUBLIC
        Visibilities.Protected -> Visibility.PROTECTED
        Visibilities.Internal -> Visibility.INTERNAL
        Visibilities.Private -> Visibility.PRIVATE
        else -> error("Unexpected visibility: $this")
    }

private inline val CirHasModality.kmModality: KmModality
    get() = when (modality) {
        Modality.FINAL -> KmModality.FINAL
        Modality.ABSTRACT -> KmModality.ABSTRACT
        Modality.OPEN -> KmModality.OPEN
        Modality.SEALED -> KmModality.SEALED
    }

private inline val CallableMemberDescriptor.Kind.kmMemberKind: MemberKind
    get() = when (this) {
        CallableMemberDescriptor.Kind.DECLARATION -> MemberKind.DECLARATION
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> MemberKind.FAKE_OVERRIDE
        CallableMemberDescriptor.Kind.DELEGATION -> MemberKind.DELEGATION
        CallableMemberDescriptor.Kind.SYNTHESIZED -> MemberKind.SYNTHESIZED
    }

private inline val CirClass.kmClassKind: KmClassKind
    get() {
        if (isCompanion) return KmClassKind.COMPANION_OBJECT
        return when (kind) {
            ClassKind.CLASS -> KmClassKind.CLASS
            ClassKind.INTERFACE -> KmClassKind.INTERFACE
            ClassKind.ENUM_CLASS -> KmClassKind.ENUM_CLASS
            ClassKind.ENUM_ENTRY -> KmClassKind.ENUM_ENTRY
            ClassKind.ANNOTATION_CLASS -> KmClassKind.ANNOTATION_CLASS
            ClassKind.OBJECT -> KmClassKind.OBJECT
        }
    }



