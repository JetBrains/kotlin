/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf


private val originClassIdKey = extrasKeyOf<ClassId?>()

/**
 * Tracks the Kotlin origin associated with [this] [ObjCType].
 * Providing the [originClassId] can signal a header generation that the class associated with the [ClassId] should
 * also be translated for the header.
 */
internal val ObjCType.originClassId: ClassId?
    get() {
        extras[originClassIdKey]?.let { return it }

        if (this is ObjCNullableReferenceType) {
            return this.nonNullType.extras[originClassIdKey]?.let { return it }
        }

        return null
    }

/**
 * See [ObjCType.originClassId]
 */
internal var MutableExtras.originClassId: ClassId?
    get() = this[originClassIdKey]
    set(value) {
        this[originClassIdKey] = value
    }