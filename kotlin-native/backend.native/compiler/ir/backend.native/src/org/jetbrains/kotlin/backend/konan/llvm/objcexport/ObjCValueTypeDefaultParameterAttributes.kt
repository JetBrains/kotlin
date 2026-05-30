/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import org.jetbrains.kotlin.backend.konan.llvm.LlvmParameterAttribute
import org.jetbrains.kotlin.backend.konan.objcexport.*

internal val ObjCValueType.defaultParameterAttributes: List<LlvmParameterAttribute>
    get() = when (this) {
        ObjCValueType.BOOL -> [LlvmParameterAttribute.SignExt]
        ObjCValueType.UNICHAR -> [LlvmParameterAttribute.ZeroExt]
        ObjCValueType.CHAR -> [LlvmParameterAttribute.SignExt]
        ObjCValueType.SHORT -> [LlvmParameterAttribute.SignExt]
        ObjCValueType.UNSIGNED_CHAR -> [LlvmParameterAttribute.ZeroExt]
        ObjCValueType.UNSIGNED_SHORT -> [LlvmParameterAttribute.ZeroExt]
        else -> []
    }
