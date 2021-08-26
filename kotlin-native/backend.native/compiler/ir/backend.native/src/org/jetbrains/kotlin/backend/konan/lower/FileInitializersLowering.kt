/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.llvm.FieldStorageKind
import org.jetbrains.kotlin.backend.konan.llvm.needsGCRegistration
import org.jetbrains.kotlin.backend.konan.llvm.storageKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal object DECLARATION_ORIGIN_MODULE_GLOBAL_INITIALIZER : IrDeclarationOriginImpl("MODULE_GLOBAL_INITIALIZER")
internal object DECLARATION_ORIGIN_MODULE_THREAD_LOCAL_INITIALIZER : IrDeclarationOriginImpl("MODULE_THREAD_LOCAL_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER : IrDeclarationOriginImpl("FILE_GLOBAL_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER : IrDeclarationOriginImpl("FILE_THREAD_LOCAL_INITIALIZER")
internal object DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER : IrDeclarationOriginImpl("FILE_STANDALONE_THREAD_LOCAL_INITIALIZER")

internal val IrFunction.isFileInitializer: Boolean
    get() = origin == DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER
            || origin == DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
            || origin == DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER

internal fun IrBuilderWithScope.irCallFileInitializer(initializer: IrFunctionSymbol) =
        irCall(initializer)

internal val IrField.shouldBeInitializedEagerly: Boolean
    get() {
        val annotations = correspondingPropertySymbol?.owner?.annotations ?: annotations
        return annotations.hasAnnotation(KonanFqNames.eagerInitialization)
    }

// TODO: ExplicitlyExported for IR proto are not longer needed.
internal class FileInitializersLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        var requireGlobalInitializer = false
        var requireThreadLocalInitializer = false
        for (declaration in irFile.declarations) {
            val irField = (declaration as? IrField) ?: (declaration as? IrProperty)?.backingField
            if (irField == null || !irField.needsInitializationAtRuntime || irField.shouldBeInitializedEagerly) continue
            if (irField.storageKind != FieldStorageKind.THREAD_LOCAL) {
                requireGlobalInitializer = true
            } else {
                requireThreadLocalInitializer = true // Either marked with thread local or only main thread visible.
            }
        }
        // TODO: think about pure initializers.
        if (!requireGlobalInitializer && !requireThreadLocalInitializer) {
            return
        }

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
                val body = declaration.body ?: return declaration
                val statements = (body as IrBlockBody).statements
                context.createIrBuilder(declaration.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                    // The order of calling initializers: first global, then thread-local.
                    // It is ok for a thread local top level property to reference a global, but not vice versa.
                    threadLocalInitFunction?.let { statements.add(0, irCallFileInitializer(it.symbol)) }
                    globalInitFunction?.let { statements.add(0, irCallFileInitializer(it.symbol)) }
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

    private val IrField.needsInitializationAtRuntime: Boolean
        get() = hasNonConstInitializer || needsGCRegistration(context)

    private val IrField.hasNonConstInitializer: Boolean
        get() {
            val it = initializer?.expression ?: return false
            if (it !is IrConst<*>) return true
            if (it.kind != IrConstKind.Null && it.type != this.type) return true // Might be boxed, so codegen won't treat it as const.
            return false
        }
}
