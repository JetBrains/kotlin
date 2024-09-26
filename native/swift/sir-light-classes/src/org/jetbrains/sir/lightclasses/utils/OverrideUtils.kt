/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils;

import org.jetbrains.kotlin.sir.*

internal inline val <reified T : SirClassMemberDeclaration> T.overridableCandidates: List<T>
    get() =
        generateSequence((parent as? SirClass)?.superClassDeclaration) { it.superClassDeclaration }
            .flatMap { it.declarations }
            .filterIsInstance<T>()
            .filter { it.modality != SirModality.FINAL }
            .toList()

private val SirClass.superClassDeclaration: SirClass? get() = (superClass as? SirNominalType)?.typeDeclaration as? SirClass

internal fun SirType.isSuitableForCovariantOverrideOf(other: SirType): Boolean = when (this) {
    is SirOptionalType -> (other as? SirOptionalType)?.let { wrappedType.isSuitableForCovariantOverrideOf(it.wrappedType) } ?: false
    is SirNominalType -> when (other) {
        is SirOptionalType -> this.isSuitableForCovariantOverrideOf(other.wrappedType)
        is SirNominalType -> this.typeDeclaration.isSubclassOf(other.typeDeclaration)
        else -> false
    }
    else -> false
}

private fun SirDeclaration.isSubclassOf(other: SirDeclaration): Boolean = this == other || this is SirClass && (superClass as? SirNominalType)?.typeDeclaration?.isSubclassOf(other) ?: false
