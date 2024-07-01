/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCReferenceType
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

private val requiresForwardDeclarationKey = extrasKeyOf<Boolean>("isForwardDeclaration")

/**
 * Marks a type such that the generated header renders a forward declaration to this type when used.
 * - Default value: `false`.
 * - Example: All types used in function and method signature are expected to render forward declarations
 */
internal val ObjCReferenceType.requiresForwardDeclaration: Boolean
    get() =
        when (this) {
            is ObjCNonNullReferenceType -> extras[requiresForwardDeclarationKey] ?: false
            is ObjCNullableReferenceType -> extras[requiresForwardDeclarationKey] ?: nonNullType.requiresForwardDeclaration
        }

/**
 * ⚠️ Marks [this] [ObjCReferenceType] as 'requires forward declaration' and returns the same instance.
 * This method shall be used during the construction of a new type.
 * @see ObjCReferenceType.requiresForwardDeclaration
 */
internal var MutableExtras.requiresForwardDeclaration: Boolean
    get() = this[requiresForwardDeclarationKey] ?: false
    set(value) {
        this[requiresForwardDeclarationKey] = value
    }