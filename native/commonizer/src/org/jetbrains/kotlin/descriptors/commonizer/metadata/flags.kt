/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.flagsOf
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.resolve.constants.NullValue

internal const val NO_FLAGS: Flags = 0

internal fun CirFunction.functionFlags(isExpect: Boolean): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityFlag,
        modalityFlag,
        memberKindFlag,
        Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES.takeIf { !hasStableParameterNames },
        Flag.Function.IS_EXPECT.takeIf { isExpect }
    ) or modifiers.modifiersFlags

internal fun CirProperty.propertyFlags(isExpect: Boolean): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityFlag,
        modalityFlag,
        memberKindFlag,
        Flag.Property.HAS_GETTER.takeIf { getter != null },
        Flag.Property.HAS_SETTER.takeIf { setter != null },
        Flag.Property.IS_DELEGATED.takeIf { isDelegate },
        Flag.Property.IS_EXPECT.takeIf { isExpect }
    ) or modifiersFlags

internal fun CirPropertyAccessor.propertyAccessorFlags(
    visibilityHolder: CirHasVisibility,
    modalityHolder: CirHasModality
): Flags {
    return flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityHolder.visibilityFlag,
        modalityHolder.modalityFlag,
        Flag.PropertyAccessor.IS_NOT_DEFAULT.takeIf { !isDefault },
        Flag.PropertyAccessor.IS_EXTERNAL.takeIf { isExternal },
        Flag.PropertyAccessor.IS_INLINE.takeIf { isInline }
    )
}

internal fun CirClassConstructor.classConstructorFlags(): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityFlag,
        Flag.Constructor.IS_SECONDARY.takeIf { !isPrimary },
        Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES.takeIf { !hasStableParameterNames }
    )

internal fun CirType.typeFlags(): Flags =
    flagsOfNotNull(
        nullableFlag,
        //Flag.Type.IS_SUSPEND.takeIf { false }
    )

internal fun CirTypeParameter.typeParameterFlags(): Flags =
    flagsOfNotNull(
        Flag.TypeParameter.IS_REIFIED.takeIf { isReified }
    )

internal fun CirValueParameter.valueParameterFlags(): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        Flag.ValueParameter.DECLARES_DEFAULT_VALUE.takeIf { declaresDefaultValue },
        Flag.ValueParameter.IS_CROSSINLINE.takeIf { isCrossinline },
        Flag.ValueParameter.IS_NOINLINE.takeIf { isNoinline }
    )

internal fun CirClass.classFlags(isExpect: Boolean): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityFlag,
        modalityFlag,
        classKindFlag,
        Flag.Class.IS_COMPANION_OBJECT.takeIf { isCompanion },
        Flag.Class.IS_INNER.takeIf { isInner },
        Flag.Class.IS_DATA.takeIf { isData },
        Flag.Class.IS_EXTERNAL.takeIf { isExternal },
        Flag.Class.IS_EXPECT.takeIf { isExpect },
        Flag.Class.IS_INLINE.takeIf { isInline },
        //Flag.Class.IS_FUN.takeIf { false }
    )

internal fun CirTypeAlias.typeAliasFlags(): Flags =
    flagsOfNotNull(
        hasAnnotationsFlag,
        visibilityFlag
    )

private inline val CirHasAnnotations.hasAnnotationsFlag: Flag?
    get() = if (annotations.isNotEmpty()) Flag.Common.HAS_ANNOTATIONS else null

private inline val CirHasVisibility.visibilityFlag: Flag
    get() = when (visibility) {
        DescriptorVisibilities.PUBLIC -> Flag.Common.IS_PUBLIC
        DescriptorVisibilities.PROTECTED -> Flag.Common.IS_PROTECTED
        DescriptorVisibilities.INTERNAL -> Flag.Common.IS_INTERNAL
        DescriptorVisibilities.PRIVATE -> Flag.Common.IS_PRIVATE
        else -> error("Unexpected visibility: $this")
    }

private inline val CirHasModality.modalityFlag: Flag
    get() = when (modality) {
        Modality.FINAL -> Flag.Common.IS_FINAL
        Modality.ABSTRACT -> Flag.Common.IS_ABSTRACT
        Modality.OPEN -> Flag.Common.IS_OPEN
        Modality.SEALED -> Flag.Common.IS_SEALED
    }

private inline val CirFunction.memberKindFlag: Flag
    get() = when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION -> Flag.Function.IS_DECLARATION
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> Flag.Function.IS_FAKE_OVERRIDE
        CallableMemberDescriptor.Kind.DELEGATION -> Flag.Function.IS_DELEGATION
        CallableMemberDescriptor.Kind.SYNTHESIZED -> Flag.Function.IS_SYNTHESIZED
    }

private inline val CirProperty.memberKindFlag: Flag
    get() = when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION -> Flag.Property.IS_DECLARATION
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> Flag.Property.IS_FAKE_OVERRIDE
        CallableMemberDescriptor.Kind.DELEGATION -> Flag.Property.IS_DELEGATION
        CallableMemberDescriptor.Kind.SYNTHESIZED -> Flag.Property.IS_SYNTHESIZED
    }

private inline val CirClass.classKindFlag: Flag
    get() = when (kind) {
        ClassKind.CLASS -> Flag.Class.IS_CLASS
        ClassKind.INTERFACE -> Flag.Class.IS_INTERFACE
        ClassKind.ENUM_CLASS -> Flag.Class.IS_ENUM_CLASS
        ClassKind.ENUM_ENTRY -> Flag.Class.IS_ENUM_ENTRY
        ClassKind.ANNOTATION_CLASS -> Flag.Class.IS_ANNOTATION_CLASS
        ClassKind.OBJECT -> Flag.Class.IS_OBJECT
    }

private inline val CirFunctionModifiers.modifiersFlags: Flags
    get() = flagsOfNotNull(
        Flag.Function.IS_OPERATOR.takeIf { isOperator },
        Flag.Function.IS_INFIX.takeIf { isInfix },
        Flag.Function.IS_INLINE.takeIf { isInline },
        Flag.Function.IS_TAILREC.takeIf { isTailrec },
        Flag.Function.IS_SUSPEND.takeIf { isSuspend },
        Flag.Function.IS_EXTERNAL.takeIf { isExternal }
    )

private inline val CirProperty.modifiersFlags: Flags
    get() = flagsOfNotNull(
        Flag.Property.IS_VAR.takeIf { isVar },
        Flag.Property.IS_CONST.takeIf { isConst },
        Flag.Property.HAS_CONSTANT.takeIf { compileTimeInitializer.takeIf { it !is NullValue } != null },
        Flag.Property.IS_LATEINIT.takeIf { isLateInit },
        Flag.Property.IS_EXTERNAL.takeIf { isExternal }
    )

private inline val CirType.nullableFlag: Flag?
    get() {
        val isNullable = when (this) {
            is CirSimpleType -> isMarkedNullable
            is CirFlexibleType -> lowerBound.isMarkedNullable
        }

        return if (isNullable) Flag.Type.IS_NULLABLE else null
    }

private fun flagsOfNotNull(vararg flags: Flag?): Flags = flagsOf(*listOfNotNull(*flags).toTypedArray())
