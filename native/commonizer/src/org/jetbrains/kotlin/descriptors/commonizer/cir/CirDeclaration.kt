/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

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

// TODO: merge with [CirDeclarationWithVisibility]
interface CirHasVisibility {
    val visibility: Visibility
}

interface CirDeclarationWithVisibility : CirDeclaration, CirHasVisibility

interface CirDeclarationWithModality : CirDeclaration {
    val modality: Modality
}

interface CirMaybeCallableMemberOfClass : CirDeclaration {
    val containingClassDetails: CirContainingClassDetails? // null assumes no containing class
}

interface CirDeclarationWithTypeParameters : CirDeclaration {
    val typeParameters: List<CirTypeParameter>
}

interface CirCallableMemberWithParameters {
    val valueParameters: List<CirValueParameter>
    val hasStableParameterNames: Boolean
    val hasSynthesizedParameterNames: Boolean
}

/**
 * Indicates a declaration that could be completely lifted up to "common" fragment.
 * NOTE: Interning can't be applied to the whole lifted declaration, only to its parts!
 */
interface CirLiftedUpDeclaration : CirDeclaration {
    val isLiftedUp: Boolean
}

/** Indicates presence of recursion in lazy calculations. */
interface CirRecursionMarker : CirDeclaration
