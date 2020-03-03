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

internal val STM_CONTEXT = Name.identifier("STMContext")

internal val UNIVERSAL_DELEGATE = Name.identifier("UniversalDelegate")

internal val STM_SEARCHER = Name.identifier("STMSearcher")

internal val SEARCH_STM_METHOD = Name.identifier("getSTM")

internal val GET_CONTEXT = Name.identifier("getContext")

internal val WRAP_METHOD = Name.identifier("wrap")

internal val GET_VAR_METHOD = Name.identifier("getVar")

internal val SET_VAR_METHOD = Name.identifier("setVar")

internal val RUN_ATOMICALLY_METHOD = Name.identifier("runAtomically")

internal val SHARED_MUTABLE_ANNOTATION = FqName("$STM_PACKAGE.SharedMutable")

internal val ATOMIC_FUNCTION_ANNOTATION_NAME = Name.identifier("AtomicFunction")

internal val ATOMIC_FUNCTION_ANNOTATION = FqName("$STM_PACKAGE.${ATOMIC_FUNCTION_ANNOTATION_NAME.identifier}")

internal val STM_CONTEXT_CLASS = FqName("$STM_PACKAGE.${STM_CONTEXT.identifier}")

internal val SHARABLE_NAME_SUFFIX = "\$_______Sharable____"

internal val STM_FIELD_NAME = "_______stm_____"

fun Name.isSharable() = this.asString().endsWith(SHARABLE_NAME_SUFFIX)
fun Name.isSTMFieldName() = this.asString().startsWith(STM_FIELD_NAME)

internal val VAR_ACCESS_TEMPORARY_PREFIX = "______var___access__________"

const val GET_PREFIX = "_get_"
const val SET_PREFIX = "_set_"

const val KT_DEFAULT_GET_PREFIX = "<get"
const val KT_DEFAULT_SET_PREFIX = "<set"

internal fun Name.isSharableViaStmDeclaration() = this.asString().endsWith(org.jetbrains.kotlinx.stm.compiler.SHARABLE_NAME_SUFFIX)

internal fun ClassDescriptor.getParentClass(): ClassDescriptor? =
    this.containingDeclaration as? ClassDescriptor

internal fun ClassDescriptor.findMethods(name: Name): List<SimpleFunctionDescriptor> =
    this.unsubstitutedMemberScope
        .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
        .filter { it.name == name }