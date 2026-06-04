/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import hair.compilation.FunctionCompilation
import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.hair.HairToBitcode
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.processBindClassToObjCNameAnnotations
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ReifiedFunctionLowering.Companion.isReifiedInline
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.SourceInfoType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

private val thisName = Name.special("<this>")
private val underscoreThisName = Name.identifier("_this")
private val doubleUnderscoreThisName = Name.identifier("__this")

/**
 * HACK: this is workaround for GH-2316, to let IDE some how operate with this.
 * We're experiencing issue with libclang which is used as compiler of expression in lldb
 * for current state support Kotlin in lldb:
 *   1. <this> isn't accepted by libclang as valid variable name.
 *   2. this is reserved name and compiled in special way.
 */
private fun IrValueDeclaration.debugNameConversion(): Name {
    if (name == thisName) {
        return when (origin) {
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER -> doubleUnderscoreThisName
            else -> underscoreThisName
        }
    }
    return name
}

internal class LocationInfo(val scope: DIScopeOpaqueRef,
                            val line: Int,
                            val column: Int,
                            val inlinedAt: LocationInfo? = null)

