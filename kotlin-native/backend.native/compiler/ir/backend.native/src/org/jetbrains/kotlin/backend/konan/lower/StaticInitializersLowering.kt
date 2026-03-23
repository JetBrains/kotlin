/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.llvm.FieldStorageKind
import org.jetbrains.kotlin.backend.konan.llvm.storageKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal object StaticInitializersOrigins {
    internal val STATIC_GLOBAL_INITIALIZER by IrDeclarationOriginImpl.Synthetic
    internal val STATIC_THREAD_LOCAL_INITIALIZER by IrDeclarationOriginImpl.Synthetic
    internal val STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER by IrDeclarationOriginImpl.Synthetic

    internal val EAGER_STATIC_GLOBAL_INITIALIZER by IrDeclarationOriginImpl.Synthetic
    internal val EAGER_STATIC_THREAD_LOCAL_INITIALIZER by IrDeclarationOriginImpl.Synthetic

    internal val INITIALIZE_GLOBAL_FIELD by IrStatementOriginImpl
    internal val INITIALIZE_THREAD_LOCAL_FIELD by IrStatementOriginImpl
}

internal val IrFunction.isGlobalStaticInitializer: Boolean
    get() = origin == StaticInitializersOrigins.STATIC_GLOBAL_INITIALIZER
internal val IrFunction.isThreadLocalStaticInitializer: Boolean
    get() = origin == StaticInitializersOrigins.STATIC_THREAD_LOCAL_INITIALIZER
            || origin == StaticInitializersOrigins.STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER
internal val IrFunction.isLazyStaticInitializer: Boolean
    get() = isGlobalStaticInitializer || isThreadLocalStaticInitializer

internal val IrFunction.isEagerStaticInitializer: Boolean
    get() = origin == StaticInitializersOrigins.EAGER_STATIC_GLOBAL_INITIALIZER
            || origin == StaticInitializersOrigins.EAGER_STATIC_THREAD_LOCAL_INITIALIZER


internal fun ConfigChecks.shouldBeInitializedEagerly(irField: IrField): Boolean {
    if (irField.parent is IrFile || irField.correspondingPropertySymbol?.owner?.parent is IrFile) {
        if (!useLazyFileInitializers()) return true
    }
    val annotations = irField.correspondingPropertySymbol?.owner?.annotations ?: irField.annotations
    return annotations.hasAnnotation(KonanFqNames.eagerInitialization)
}

internal class StaticInitializersLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
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

    private fun IrStatement.isConst(): Boolean = when (this) {
        is IrConst, is IrConstantValue -> true
        is IrBlock -> {
            if (statements.isEmpty())
                true
            else {
                // This might happen after the local declarations lowering where local declarations are replaced with an empty composite.
                statements.take(statements.size - 1).all { it is IrComposite && it.statements.isEmpty() }
                        && statements.last().isConst()
            }
        }
        else -> false
    }

    fun processDeclarationContainter(container: IrDeclarationContainer) {
        val threadLocalInitializers = mutableListOf<IrExpression>()
        val globalInitializers = mutableListOf<IrExpression>()
        val eagerThreadLocalInitializers = mutableListOf<IrExpression>()
        val eagerGlobalInitializers = mutableListOf<IrExpression>()

        val builder = context.irBuiltIns.createIrBuilder((container as IrSymbolOwner).symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)

        for (declaration in container.declarations) {
            val irField = (declaration as? IrField) ?: (declaration as? IrProperty)?.backingField
            if (irField == null || !irField.isStatic) continue
            val initializer = irField.initializer?.expression
            val isThreadLocal = irField.storageKind == FieldStorageKind.THREAD_LOCAL
            /**
             * Optimization: initialize constantly to avoid runtime checks if possible.
             *
             * To do it, we need to know initializer in compile time, and the field must be
             * safe to be ignored by garbage collector.
             *
             * The latter is the case if it's either a final field initialized by constant or
             * a field of a primitive type.
             *
             * Potentially, we could use this optimization for final thread-local fields initialized
             * by constants, but it's not supported in code generation, and it's not clear why you would
             * have such a field.
             */
            if (initializer?.isConst() == true && !isThreadLocal) {
                if (!irField.type.binaryTypeIsReference() || irField.isFinal) {
                    continue
                }
            }
            val realInitializer = when {
                initializer != null -> initializer
                irField.type.binaryTypeIsReference() -> builder.irNull() // we need to initialize with something to register with GC
                else -> continue
            }
            val isEager = context.shouldBeInitializedEagerly(irField)
            val initializers = when (isThreadLocal) {
                false if isEager -> eagerGlobalInitializers
                true if isEager -> eagerThreadLocalInitializers
                false -> globalInitializers
                true -> threadLocalInitializers
            }
            initializers.add(builder.irSetField(
                    receiver = null,
                    field = irField,
                    value = realInitializer,
                    origin = if (isThreadLocal) StaticInitializersOrigins.INITIALIZE_THREAD_LOCAL_FIELD else StaticInitializersOrigins.INITIALIZE_GLOBAL_FIELD
            ))
            irField.initializer = null
        }

        val globalInitFunction = runIf(globalInitializers.isNotEmpty()) {
            buildInitFunction(
                    container = container,
                    name = $$"$init_global",
                    origin = StaticInitializersOrigins.STATIC_GLOBAL_INITIALIZER,
                    initializers = globalInitializers
            )
        }
        val threadLocalInitFunction = runIf(threadLocalInitializers.isNotEmpty()) {
            buildInitFunction(
                    container = container,
                    name = $$"$init_thread_local",
                    origin = when {
                        globalInitializers.isNotEmpty() -> StaticInitializersOrigins.STATIC_THREAD_LOCAL_INITIALIZER
                        else -> StaticInitializersOrigins.STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER
                    },
                    initializers = threadLocalInitializers
            )
        }

        runIf(eagerGlobalInitializers.isNotEmpty()) {
            buildInitFunction(
                    container = container,
                    name = $$"$init_global_eager",
                    origin = StaticInitializersOrigins.EAGER_STATIC_GLOBAL_INITIALIZER,
                    initializers = eagerGlobalInitializers
            )
        }

        runIf(eagerThreadLocalInitializers.isNotEmpty()) {
            buildInitFunction(
                    container = container,
                    name = $$"$init_thread_local_eager",
                    origin = StaticInitializersOrigins.EAGER_STATIC_THREAD_LOCAL_INITIALIZER,
                    initializers = eagerThreadLocalInitializers
            )
        }

        if (globalInitFunction == null && threadLocalInitFunction == null) return
        
        fun IrFunction.addInitializersCall() {
            val body = body ?: return
            val statements = (body as IrBlockBody).statements
            context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                // The order of calling initializers: first global, then thread-local.
                // It is ok for a thread local top level property to reference a global, but not vice versa.
                threadLocalInitFunction?.let { statements.add(0, irCall(it.symbol)) }
                globalInitFunction?.let { statements.add(0, irCall(it.symbol)) }
            }
        }

        for (function in container.simpleFunctions()) {
            if (function.dispatchReceiverParameter != null) continue // already initialized when instance was created
            if (function.origin == DECLARATION_ORIGIN_ENTRY_POINT) continue // is not really in any class
            if (function.isLazyStaticInitializer || function.isEagerStaticInitializer) continue // don't initialize recursively
            function.addInitializersCall()
        }
        if (container is IrClass) {
            for (constructor in container.constructors) {
                constructor.addInitializersCall()
            }
        }
    }

    private fun buildInitFunction(
            container: IrDeclarationContainer,
            name: String,
            origin: IrDeclarationOrigin,
            initializers: List<IrExpression>
    ) = context.irFactory.buildFun {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        this.origin = origin
        // File name is added to avoid having same-named top-level private functions in the same package.
        // While llvm handles it by adding suffix automatically, it still leads to less predictable binaries.
        // With adding container.name to name, it would clash only if you have the same file name in the same package,
        // which is a much less common case, because it doesn't work on jvm.
        // For classes, it's not necessary, as the class fqname would be added to the mangled function name anyway
        this.name = if (container is IrFile) Name.identifier(name + '$' + container.name) else Name.identifier(name)
        visibility = DescriptorVisibilities.PRIVATE
        returnType = context.irBuiltIns.unitType
    }.apply {
        parent = container
        body = context.irFactory.createBlockBody(startOffset, endOffset, initializers)
                .setDeclarationsParent(this)
        container.declarations.add(0, this)
    }

}
