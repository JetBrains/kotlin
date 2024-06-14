/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanFqNames

context(KaSession)
internal fun KaType.getInlineTargetTypeOrNull(): KaType? {
    if (this !is KaClassType) return null
    val classSymbol = symbol as? KaNamedClassOrObjectSymbol ?: return null
    return classSymbol.getInlineTargetTypeOrNull()?.markNullableIf(isMarkedNullable)
}

context(KaSession)
internal fun KaNamedClassOrObjectSymbol.getInlineTargetTypeOrNull(): KaType? {
    if (!isInlineIncludingKotlinNativeSpecialClasses()) return null

    val constructor = declaredMemberScope.constructors
        .find { constructor -> constructor.isPrimary && constructor.valueParameters.size == 1 }
        ?: return null

    val inlinedType = constructor.valueParameters.single().returnType

    /* What if this type is also inline type? */
    val inlineInlineType = inlinedType.getInlineTargetTypeOrNull()
    if (inlineInlineType != null) {
        return inlineInlineType
    }

    return inlinedType
}

/**
 * Kotlin/Native specific implementation for testing if a certain class can be considered 'inline'.
 * Classes that are marked as 'inline' (e.g. ```value class X(val value: Int)```) will be considered inline (of course).
 * However, there seemingly exist classes like 'kotlin.native.internal.NativePtr' which shall also be considered 'inline'
 * despite no modifier being present. This is considered a 'special Kotlin Native' class in the context of this function.
 */
context(KaSession)
private fun KaNamedClassOrObjectSymbol.isInlineIncludingKotlinNativeSpecialClasses(): Boolean {
    if (this.isInline) return true
    val classId = classId ?: return false

    /* Top Level symbols can be special K/N types */
    if (containingSymbol is KaClassOrObjectSymbol) return false

    if (classId.packageFqName == KonanFqNames.internalPackageName && classId.shortClassName == KonanFqNames.nativePtr.shortName()) {
        return true
    }

    if (classId.packageFqName == InteropFqNames.packageName && classId.shortClassName == InteropFqNames.cPointer.shortName()) {
        return true
    }

    return false
}

context(KaSession)
private fun KaType.markNullable(): KaType {
    if (this.nullability == KaTypeNullability.NULLABLE) return this
    return this.withNullability(KaTypeNullability.NULLABLE)
}

context(KaSession)
private fun KaType.markNullableIf(shouldMarkNullable: Boolean): KaType {
    return if (shouldMarkNullable) markNullable() else this
}