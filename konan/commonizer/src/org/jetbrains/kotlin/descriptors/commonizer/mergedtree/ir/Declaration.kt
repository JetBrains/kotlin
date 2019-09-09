/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * An intermediate representation of [DeclarationDescriptor]s for commonization purposes.
 *
 * The most essential subclasses are:
 * - [ClassDeclaration] - represents [ClassDescriptor]
 * - [TypeAlias] - [TypeAliasDescriptor]
 * - [Function] - [SimpleFunctionDescriptor]
 * - [Property] - [PropertyDescriptor]
 * - [Package] - union of multiple [PackageFragmentDescriptor]s with the same [FqName] contributed by commonized [ModuleDescriptor]s
 * - [Module] - [ModuleDescriptor]
 * - [Root] - the root of the whole IR tree
 */
interface Declaration

interface AnnotatedDeclaration : Declaration {
    val annotations: Annotations
}

interface NamedDeclaration : Declaration {
    val name: Name
}

interface DeclarationWithVisibility : Declaration {
    val visibility: Visibility
}

interface MaybeVirtualCallableMember : DeclarationWithVisibility {
    val isVirtual: Boolean
}

interface DeclarationWithTypeParameters : Declaration {
    val typeParameters: List<TypeParameter>
}

/** Indicates presence of recursion in lazy calculations. */
interface RecursionMarker : Declaration

@Suppress("unused")
internal fun Declaration.unsupported(): Nothing = error("This method should never be called")
