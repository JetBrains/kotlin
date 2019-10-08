/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * An intermediate representation of [DeclarationDescriptor]s for commonization purposes.
 *
 * The most essential subclasses are:
 * - [CirClass] - represents [ClassDescriptor]
 * - [CirTypeAlias] - [TypeAliasDescriptor]
 * - [CirFunction] - [SimpleFunctionDescriptor]
 * - [CirProperty] - [PropertyDescriptor]
 * - [CirPackage] - union of multiple [PackageFragmentDescriptor]s with the same [FqName] contributed by commonized [ModuleDescriptor]s
 * - [CirModule] - [ModuleDescriptor]
 * - [CirRoot] - the root of the whole Commonizer IR tree
 */
interface CirDeclaration

interface CirAnnotatedDeclaration : CirDeclaration {
    val annotations: List<CirAnnotation>
}

interface CirNamedDeclaration : CirDeclaration {
    val name: Name
}

interface CirDeclarationWithVisibility : CirDeclaration {
    val visibility: Visibility
}

interface CirDeclarationWithModality : CirDeclaration {
    val modality: Modality
}

interface CirMaybeCallableMemberOfClass : CirDeclaration {
    val containingClassKind: ClassKind? // null assumes no containing class
    val containingClassModality: Modality? // null assumes no containing class
    val containingClassIsData: Boolean? // null assumes no containing class
}

interface CirDeclarationWithTypeParameters : CirDeclaration {
    val typeParameters: List<CirTypeParameter>
}

/** Indicates presence of recursion in lazy calculations. */
interface CirRecursionMarker : CirDeclaration

@Suppress("unused", "NOTHING_TO_INLINE")
internal inline fun CirDeclaration.unsupported(): Nothing = error("This method should never be called")
