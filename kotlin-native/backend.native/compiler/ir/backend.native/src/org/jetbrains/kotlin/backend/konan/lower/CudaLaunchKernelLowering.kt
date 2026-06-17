/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.buildCudaKernelExportName
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name

/**
 * Rewrites every `CudaLaunchpad.launch(::kernel, p1, …, pN)` — i.e. an `IrCall` whose callee
 * carries `@CudaLaunchKernel` — into a direct call to the internal `launchKernel(name, grid,
 * block, sharedMemBytes, stream, vararg args)`. The kernel's mangled symbol name is computed
 * from the `IrFunctionReference` passed as `ref` via `buildCudaKernelExportName`, the same util
 * that `AssignCudaKernelExportNamesPhase` (`CudaCrossModuleInlining.kt`) uses to attach
 * `@ExportForCppRuntime` on the device side — so the host's string literal and the device's
 * PTX symbol name are guaranteed to match.
 *
 * Position constraint: must run AFTER `UpgradeCallableReferences` (which rewrites the raw
 * `IrFunctionReference` into `IrRichFunctionReference` carrying a `reflectionTargetSymbol`),
 * and BEFORE `NativeFunctionReferenceLowering` (which collapses the rich-reference into a
 * constructor call to a synthesized `KFunctionN`-implementing class — at which point the
 * callable-reference shape is gone and we can't read the referenced kernel off it any more).
 */
internal class CudaLaunchKernelLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrTransformer<IrSymbolOwner?>() {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrSymbolOwner?): IrStatement {
                val newScope = (declaration as? IrSymbolOwner) ?: data
                return super.visitDeclaration(declaration, newScope)
            }

            override fun visitCall(expression: IrCall, data: IrSymbolOwner?): IrElement {
                expression.transformChildren(this, data)

                val callee = expression.symbol.owner
                if (!callee.hasAnnotation(KonanFqNames.cudaLaunchKernel)) return expression

                val scopeOwner = data
                        ?: error("CudaLaunchpad.launch call outside any declaration scope: ${expression.dump()}")
                return rewriteLaunch(scopeOwner, callee, expression, irFile)
            }
        }, data = null)
    }

    private fun rewriteLaunch(
            scopeOwner: IrSymbolOwner,
            launchCallee: IrSimpleFunction,
            expression: IrCall,
            irFile: IrFile,
    ): IrExpression {
        // `launch(ref, p1, …, pN)` — `arguments[0]` is the launchpad dispatch receiver,
        // `arguments[1]` is the kernel reference, `arguments[2..]` are the pass-through args.
        val launchpad = expression.arguments[0]
                ?: error("CudaLaunchpad.launch call missing dispatch receiver: ${expression.dump()}")
        val refArg = expression.arguments[1]
                ?: error("CudaLaunchpad.launch call missing `ref` argument: ${expression.dump()}")

        // After `UpgradeCallableReferences` (runs early in the pipeline, well before us), the
        // raw `IrFunctionReference` shape for `::vecAdd` has been rewritten into an
        // `IrRichFunctionReference` whose `reflectionTargetSymbol` points at the originally
        // referenced function. Lambdas (`{ … }`) produce the same node but with a null
        // `reflectionTargetSymbol` — reject those, since there's no kernel symbol to mangle a
        // name from.
        val richRef = refArg as? IrRichFunctionReference
        val referenced = (richRef?.reflectionTargetSymbol?.owner as? IrSimpleFunction)
        if (richRef == null || referenced == null) {
            context.reportCompilationError(
                    "CudaLaunchpad.launch requires a direct function reference like `::vecAdd` as its first argument.",
                    irFile, refArg
            )
        }
        // Accept two shapes for the referenced kernel:
        //  - A top-level function: parent is `IrPackageFragment` (either an `IrFile` for
        //    in-module kernels or an `IrExternalPackageFragment` for klib-deserialized ones).
        //  - A member of an `object` declared directly at the top level of a `@CudaCompile`
        //    file: parent is `IrClass` (the object) whose parent is the file. The kernel ends
        //    up with a dispatch receiver of the object type — the device side keeps that
        //    parameter, so we prepend a 0L placeholder to the launch args below.
        val containerClass = referenced.parent as? IrClass
        val packageFragment = when {
            containerClass != null -> {
                if (!containerClass.isObject) {
                    context.reportCompilationError(
                            "CudaLaunchpad.launch requires a top-level function or a member of a top-level `object` " +
                                    "(`${referenced.name.asString()}` is a member of class `${containerClass.name.asString()}`).",
                            irFile, refArg
                    )
                }
                containerClass.parent as? IrPackageFragment ?: run {
                    context.reportCompilationError(
                            "CudaLaunchpad.launch requires the containing `object` to be top-level " +
                                    "(`${containerClass.name.asString()}` is nested).",
                            irFile, refArg
                    )
                }
            }
            else -> referenced.parent as? IrPackageFragment ?: run {
                context.reportCompilationError(
                        "CudaLaunchpad.launch requires a top-level function or a member of a top-level `object` " +
                                "(`${referenced.name.asString()}` is not).",
                        irFile, refArg
                )
            }
        }
        // Best-effort: when the referenced kernel lives in the module being compiled, the
        // package fragment is the `IrFile` and we can verify it carries `@CudaCompile`.
        // References to kernels in already-deserialized klibs come through as
        // `IrExternalPackageFragment`, which doesn't carry file annotations — skip the check
        // there and trust the user.
        val sourceFile = packageFragment as? IrFile
        if (sourceFile != null && !sourceFile.hasAnnotation(KonanFqNames.cudaCompile)) {
            context.reportCompilationError(
                    "CudaLaunchpad.launch requires a function declared in a `@CudaCompile` file " +
                            "(`${referenced.name.asString()}` is not).",
                    irFile, refArg
            )
        }

        val qualifiedName = if (containerClass != null) {
            "${containerClass.name.asString()}.${referenced.name.asString()}"
        } else {
            referenced.name.asString()
        }
        val exportName = buildCudaKernelExportName(
                packageFragment.packageFqName.asString(),
                qualifiedName,
        )

        // `pN` args follow at `arguments[2..]`; pass them straight through to the vararg.
        val passThroughArgs = expression.arguments.drop(2).map {
            it ?: error("CudaLaunchpad.launch call has a null pass-through arg: ${expression.dump()}")
        }

        val launchpadClass = launchpad.type.classOrNull?.owner
                ?: error("CudaLaunchpad receiver has no class: ${launchpad.type.render()}")
        val gridGetter = launchpadClass.findGetter("gridSize")
        val blockGetter = launchpadClass.findGetter("blockSize")
        val sharedMemGetter = launchpadClass.findGetter("sharedMemSize")
        val streamGetter = launchpadClass.findGetter("stream")

        // Capture into locals: inside the `irBlock { … }` builder lambda `this` is an
        // `IrBlockBuilder`, whose own `context` is `IrGeneratorContext` and would shadow this
        // class's `Context` property.
        val konanContext = context
        val launchKernelSymbol = konanContext.symbols.launchKernel
        val anyN = konanContext.irBuiltIns.anyNType
        val anyArrayType = konanContext.irBuiltIns.arrayClass.typeWith(anyN)
        val builder = konanContext.createIrBuilder(scopeOwner.symbol, expression.startOffset, expression.endOffset)
        return builder.irBlock(expression.startOffset, expression.endOffset) {
            val padTmp = irTemporary(launchpad, "launchpad")
            // For object-member kernels the device-side function keeps its dispatch receiver
            // parameter (an `Foo*` slot, even when nothing reads it because the object is
            // stateless and the call-site `Foo.INSTANCE` access was elided by
            // `DropTrivialObjectInstancesLowering`). Feed the slot a 0L so the runtime
            // marshaller writes 8 zero bytes for it and the subsequent `pN` args land at the
            // offsets the kernel expects. `cuLaunchKernel` then passes a null receiver, which
            // a stateless object never reads.
            val varargElements = buildList {
                if (containerClass != null) {
                    add(irLong(0L))
                }
                addAll(passThroughArgs)
            }
            +irCall(launchKernelSymbol).apply {
                arguments[0] = irString(exportName)
                arguments[1] = irCall(gridGetter).apply { arguments[0] = irGet(padTmp) }
                arguments[2] = irCall(blockGetter).apply { arguments[0] = irGet(padTmp) }
                arguments[3] = irCall(sharedMemGetter).apply { arguments[0] = irGet(padTmp) }
                arguments[4] = irCall(streamGetter).apply { arguments[0] = irGet(padTmp) }
                arguments[5] = IrVarargImpl(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        type = anyArrayType,
                        varargElementType = anyN,
                        elements = varargElements,
                )
            }
        }
    }
}

private fun IrClass.findGetter(propertyName: String): IrSimpleFunctionSymbol {
    val name = Name.identifier(propertyName)
    return properties.firstOrNull { it.name == name }?.getter?.symbol
            ?: error("CudaLaunchpad is missing property `$propertyName`")
}
