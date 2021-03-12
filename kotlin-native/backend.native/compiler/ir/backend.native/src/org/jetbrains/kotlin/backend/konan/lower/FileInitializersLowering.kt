/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.FieldStorageKind
import org.jetbrains.kotlin.backend.konan.llvm.storageKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal object DECLARATION_ORIGIN_MODULE_INITIALIZER : IrDeclarationOriginImpl("MODULE_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER : IrDeclarationOriginImpl("FILE_GLOBAL_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER : IrDeclarationOriginImpl("FILE_THREAD_LOCAL_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER : IrDeclarationOriginImpl("FILE_STANDALONE_THREAD_LOCAL_INITIALIZER")

// TODO: ExplicitlyExported for IR proto are not longer needed.
internal class FileInitializersLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        var requireGlobalInitializer = false
        var requireThreadLocalInitializer = false
        for (declaration in irFile.declarations) {
            val irField = (declaration as? IrField) ?: (declaration as? IrProperty)?.backingField
            if (irField == null || !irField.hasNonConstInitializer) continue
            if (irField.storageKind == FieldStorageKind.THREAD_LOCAL)
                requireThreadLocalInitializer = true
            else requireGlobalInitializer = true
        }
        // TODO: think about pure initializers.
        if (!requireGlobalInitializer && !requireThreadLocalInitializer) return

        val globalInitFunction =
                if (requireGlobalInitializer)
                    buildInitFileFunction(irFile, "\$init_global", DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER)
                else null
        val threadLocalInitFunction =
                if (requireThreadLocalInitializer)
                    buildInitFileFunction(irFile, "\$init_thread_local",
                            if (requireGlobalInitializer)
                                DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
                            else DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER
                    )
                else null

        irFile.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                declaration.transformChildrenVoid(this)
                // TODO: what about private functions? Can they be called from other file somehow?
                val body = declaration.body ?: return declaration
                // TODO: Create a pass to remove redundant calls to [initFile].
                threadLocalInitFunction?.let {
                    (body as IrBlockBody).statements.add(0, IrCallImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.unitType,
                            it.symbol,
                            typeArgumentsCount = 0, valueArgumentsCount = 0
                    ))
                }
                globalInitFunction?.let {
                    (body as IrBlockBody).statements.add(0, IrCallImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.unitType,
                            it.symbol,
                            typeArgumentsCount = 0, valueArgumentsCount = 0
                    ))
                }
                return declaration
            }
        })
    }

    private fun buildInitFileFunction(irFile: IrFile, name: String, origin: IrDeclarationOrigin) = context.irFactory.buildFun {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        this.origin = origin
        this.name = Name.identifier(name)
        visibility = DescriptorVisibilities.PRIVATE
        returnType = context.irBuiltIns.unitType
    }.apply {
        parent = irFile
        irFile.declarations.add(0, this)
    }

    private val IrField.hasNonConstInitializer: Boolean
        get() = initializer.let { it != null && it !is IrConst<*> }
}