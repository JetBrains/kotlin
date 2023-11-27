/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin


internal fun IrExpression.isBoxOrUnboxCall() =
        (this is IrCall && symbol.owner.origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION)

internal val IrFunctionAccessExpression.actualCallee: IrFunction
    get() {
        val callee = symbol.owner
        return ((this as? IrCall)?.superQualifierSymbol?.owner?.getOverridingOf(callee) ?: callee).target
    }


private fun IrClass.getOverridingOf(function: IrFunction) = (function as? IrSimpleFunction)?.let {
    it.allOverriddenFunctions.atMostOne { it.parent == this }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrPackageFragment.konanLibrary: KotlinLibrary?
    get() {
        if (this is IrFile) {
            val fileMetadata = metadata as? DescriptorMetadataSource.File
            val moduleDescriptor = fileMetadata?.descriptors?.singleOrNull() as? ModuleDescriptor
            moduleDescriptor?.konanLibrary?.let { return it }
        }
        return this.moduleDescriptor.konanLibrary
    }

// Any changes made to konanLibrary here should be ported to the containsDeclaration
// function in LlvmModuleSpecificationBase in LlvmModuleSpecificationImpl.kt
val IrDeclaration.konanLibrary: KotlinLibrary?
    get() {
        ((this as? IrMetadataSourceOwner)?.metadata as? KonanMetadata)?.let { return it.konanLibrary }
        return when (val parent = parent) {
            is IrPackageFragment -> parent.konanLibrary
            is IrDeclaration -> parent.konanLibrary
            else -> TODO("Unexpected declaration parent: $parent")
        }
    }

fun IrDeclaration.isFromInteropLibrary() = konanLibrary?.isInteropLibrary() == true
fun IrPackageFragment.isFromInteropLibrary() = konanLibrary?.isInteropLibrary() == true

/**
 * This function should be equivalent to [IrDeclaration.isFromInteropLibrary], but in fact it is not for declarations
 * from Fir modules. This should be fixed in the future.
 */
@ObsoleteDescriptorBasedAPI
fun IrDeclaration.isFromInteropLibraryByDescriptor() = descriptor.isFromInteropLibrary()