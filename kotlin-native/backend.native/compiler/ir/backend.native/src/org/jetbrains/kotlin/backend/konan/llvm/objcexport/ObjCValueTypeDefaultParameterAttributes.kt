/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import org.jetbrains.kotlin.backend.konan.llvm.LlvmParameterAttribute
import org.jetbrains.kotlin.backend.konan.objcexport.*

internal val ObjCValueType.defaultParameterAttributes: List<LlvmParameterAttribute>
    get() = when (this) {
        ObjCValueType.BOOL -> listOf(LlvmParameterAttribute.SignExt)
        ObjCValueType.UNICHAR -> listOf(LlvmParameterAttribute.ZeroExt)
        ObjCValueType.CHAR -> listOf(LlvmParameterAttribute.SignExt)
        ObjCValueType.SHORT -> listOf(LlvmParameterAttribute.SignExt)
        ObjCValueType.UNSIGNED_CHAR -> listOf(LlvmParameterAttribute.ZeroExt)
        ObjCValueType.UNSIGNED_SHORT -> listOf(LlvmParameterAttribute.ZeroExt)
        else -> emptyList()
    }
