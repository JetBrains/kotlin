/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val STM_PACKAGE = FqName("kotlinx.stm")

internal val STM_INTERFACE = Name.identifier("STM")

internal val STM_SEARCHER = Name.identifier("STMSearcher")

internal val SEARCH_STM_METHOD = Name.identifier("getSTM")

internal val RUN_ATOMICALLY_METHOD = Name.identifier("runAtomically")

internal val SHARED_MUTABLE_ANNOTATION = FqName("$STM_PACKAGE.SharedMutable")

internal val SHARABLE_NAME_SUFFIX = "\$_______Sharable____"

internal val STM_FIELD_NAME = "_______stm_____"

internal fun Name.isSharableViaStmDeclaration() = this.asString().endsWith(org.jetbrains.kotlinx.stm.compiler.SHARABLE_NAME_SUFFIX)

internal fun ClassDescriptor.getParentClass(): ClassDescriptor? =
    this.containingDeclaration as? ClassDescriptor

internal fun ClassDescriptor.findMethods(name: Name): List<SimpleFunctionDescriptor> =
    this.unsubstitutedMemberScope
        .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
        .filter { it.name == name }