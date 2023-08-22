/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.ConfigChecks
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENTRY_POINT
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

internal val DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER = IrDeclarationOriginImpl("STATIC_GLOBAL_INITIALIZER")
internal val DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER = IrDeclarationOriginImpl("STATIC_THREAD_LOCAL_INITIALIZER")
internal val DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER = IrDeclarationOriginImpl("STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER")

internal val IrFunction.isStaticInitializer: Boolean
    get() = origin == DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER
            || origin == DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
            || origin == DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER

internal fun IrBuilderWithScope.irCallFileInitializer(initializer: IrFunctionSymbol) =
        irCall(initializer)

internal fun ConfigChecks.shouldBeInitializedEagerly(irField: IrField): Boolean {
    if (irField.parent is IrFile || irField.correspondingPropertySymbol?.owner?.parent is IrFile) {
        if (!useLazyFileInitializers()) return true
    }
    val annotations = irField.correspondingPropertySymbol?.owner?.annotations ?: irField.annotations
    return annotations.hasAnnotation(KonanFqNames.eagerInitialization)
}

// TODO: ExplicitlyExported for IR proto are not longer needed.
internal class StaticInitializersLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }
            override fun visitFile(declaration: IrFile) {
                processDeclarationContainter(declaration)
                declaration.acceptChildrenVoid(this)
            }
            override fun visitClass(declaration: IrClass) {
                processDeclarationContainter(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    fun processDeclarationContainter(container: IrDeclarationContainer) {
        var requireGlobalInitializer = false
        var requireThreadLocalInitializer = false
        for (declaration in container.declarations) {
            val irField = (declaration as? IrField) ?: (declaration as? IrProperty)?.backingField
            if (irField == null || !irField.isStatic || !irField.needsInitializationAtRuntime || context.shouldBeInitializedEagerly(irField)) continue
            if (irField.storageKind(context) != FieldStorageKind.THREAD_LOCAL) {
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
                    buildInitFileFunction(container, "\$init_global", DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER)
                else null
        val threadLocalInitFunction =
                if (requireThreadLocalInitializer)
                    buildInitFileFunction(container, "\$init_thread_local",
                            if (requireGlobalInitializer)
                                DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
                            else DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER
                    )
                else null

        fun IrFunction.addInitializersCall() {
            val body = body ?: return
            val statements = (body as IrBlockBody).statements
            context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                // The order of calling initializers: first global, then thread-local.
                // It is ok for a thread local top level property to reference a global, but not vice versa.
                threadLocalInitFunction?.let { statements.add(0, irCallFileInitializer(it.symbol)) }
                globalInitFunction?.let { statements.add(0, irCallFileInitializer(it.symbol)) }
            }
        }

        container.simpleFunctions()
                .filter { it.dispatchReceiverParameter == null }
                .filterNot { it.origin == DECLARATION_ORIGIN_ENTRY_POINT }
                .forEach { it.addInitializersCall() }
        (container as? IrClass)?.constructors?.forEach { it.addInitializersCall() }
    }

    private fun buildInitFileFunction(container: IrDeclarationContainer, name: String, origin: IrDeclarationOrigin) = context.irFactory.buildFun {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        this.origin = origin
        this.name = Name.identifier(name)
        visibility = DescriptorVisibilities.PRIVATE
        returnType = context.irBuiltIns.unitType
    }.apply {
        parent = container
        container.declarations.add(0, this)
    }

    private val IrField.needsInitializationAtRuntime: Boolean
        get() = hasNonConstInitializer || needsGCRegistration(context)

}
