/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting") // Idea bug?

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.isInlined
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.ir.isBoxOrUnbox
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.lower.PreCodegenFunctionInlining
import org.jetbrains.kotlin.backend.konan.lower.isEagerStaticInitializer
import org.jetbrains.kotlin.backend.konan.lower.isLazyStaticInitializer
import org.jetbrains.kotlin.backend.konan.lower.originalConstructor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVirtualCall
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Greedy cross-module inliner for CUDA device kernels.
 *
 * The K/N klib carries pre-built bitcode only for the host's native target; cross-target
 * builds (e.g. `mingw_x64` from `macos_arm64`) materialize stdlib bitcode on the fly inside
 * the host fragment's codegen, and the device fragment has no equivalent path. Any call
 * from a `@CudaCompile` kernel into stdlib or another klib — `<get-rawValue>`, value-class
 * accessors, `FloatVarOf<T>.<get-value>`/`<set-value>`, `NativePtr.<constructor>`, etc. —
 * therefore surfaces in the emitted PTX as an unresolved extern.
 *
 * Walks each `@CudaCompile`-file top-level function and inlines every cross-module callee
 * (those with `konanLibrary != null`) in place, looping until no such calls remain. Calls
 * to functions in the current main module are intentionally left alone. Mutations are
 * confined to kernel function bodies — dep IR is read but never modified, so the host
 * fragment's lowerings and codegen, which share the same dep IR, stay unaffected. Virtual
 * calls are not inlined (would need a resolved target); subset validation rejects them at
 * the source, so any survivor here points at a missing lowering rather than something we
 * should silently leave behind.
 */
internal val CudaDeviceCrossModuleInliningPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "CudaDeviceCrossModuleInlining",
        op = { generationState, module ->
            val context = generationState.context
            module.files
                    .filter { it.hasAnnotation(KonanFqNames.cudaCompile) }
                    .flatMap { it.declarations }
                    .filterIsInstance<IrSimpleFunction>()
                    .filter { it.body != null }
                    .forEach { kernelFn ->
                        var iteration = 0
                        while (iteration < MAX_INLINE_ITERATIONS) {
                            // Drop any `throw` left over from a prior inlining round (e.g. an
                            // inlined `ThrowNullPointerException` body or a `!!` lowering).
                            // PTX has no exception model; the throw and the exception object
                            // it would have constructed both become unreachable and DCE-able.
                            replaceThrowsWithEarlyReturn(context, kernelFn)
                            val crossModuleCallees = collectCrossModuleCallees(kernelFn)
                            if (crossModuleCallees.isEmpty()) break

                            PreCodegenFunctionInlining(context, crossModuleCallees).run(kernelFn)
                            iteration++
                        }
                        check(iteration < MAX_INLINE_ITERATIONS) {
                            "CUDA device cross-module inlining did not converge for ${kernelFn.name} " +
                                    "after $MAX_INLINE_ITERATIONS iterations — likely a recursive cross-module call."
                        }
                        // Final pass: any throws introduced by the last inlining iteration.
                        replaceThrowsWithEarlyReturn(context, kernelFn)
                    }
        }
)

private const val MAX_INLINE_ITERATIONS = 32

/**
 * Replace each `IrThrow` reachable from [fn] with an `IrReturn` to the nearest enclosing
 * return target whose type can carry a sane default — an inlined `IrReturnableBlock` with
 * a non-`Nothing` type, or [fn] itself as the outermost fallback. Nothing-typed return
 * targets (the immediate result of inlining a `Nothing`-returning function like
 * `ThrowNullPointerException`) are skipped because they carry no constructible default.
 *
 * This preserves control-flow semantics — `throw` is a non-returning exit, so leaving the
 * surrounding scope via an early return matches it best. Replacing with an empty composite
 * (as an earlier iteration did) silently let execution fall through past the throw, which
 * is wrong wherever the surrounding expression depended on the throw not reaching it.
 */
private fun replaceThrowsWithEarlyReturn(context: Context, fn: IrSimpleFunction) {
    fn.body?.transformChildren(object : IrTransformer<IrReturnTargetSymbol>() {
        override fun visitElement(element: IrElement, data: IrReturnTargetSymbol): IrElement {
            element.transformChildren(this, data)
            return element
        }

        override fun visitReturnableBlock(expression: IrReturnableBlock, data: IrReturnTargetSymbol): IrExpression {
            val nestedData = if (expression.type.isNothing()) data else expression.symbol
            expression.transformChildren(this, nestedData)
            return expression
        }

        override fun visitThrow(expression: IrThrow, data: IrReturnTargetSymbol): IrExpression {
            val returnType = data.returnType
            val builder = context.createIrBuilder(data, expression.startOffset, expression.endOffset)
            val value = if (returnType.isUnit()) {
                builder.irCall(context.symbols.theUnitInstance, context.irBuiltIns.unitType)
            } else {
                IrConstImpl.defaultValueForType(expression.startOffset, expression.endOffset, returnType)
            }
            return builder.irReturn(value)
        }
    }, data = fn.symbol)
}

private val IrReturnTargetSymbol.returnType: IrType
    get() = when (val target = owner) {
        is IrFunction -> target.returnType
        is IrReturnableBlock -> target.type
        else -> error("Unexpected IrReturnTarget: ${target::class.simpleName}")
    }

/**
 * Replace the default mangled LLVM symbol name of each public top-level `@CudaCompile`
 * function with a short `<package>_<funcName>` form (dots in the package FQN become
 * underscores), so that host-side `cuModuleGetFunction("demo_vecAdd", …)` can address the
 * kernel without knowing K/N's signature-mangled symbol shape.
 *
 * Implementation: attach an `@ExportForCppRuntime("short_name")` annotation, which K/N's
 * `funSymbolNameImpl` already honors as a non-external mangling override (see
 * `BinaryInterface.kt` — `findManglingAnnotation`). Must run before
 * `CreateLLVMDeclarationsPhase` so the LLVM function carries the short name from the start.
 *
 * Collisions are the user's responsibility for v0: two public top-level kernels with the
 * same simple name in the same package would map to identical export symbols. Kotlin
 * disallows duplicate top-level signatures, but overloads on parameter types would
 * collapse here. A future iteration can mangle the param shape in.
 */
internal val AssignCudaKernelExportNamesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "AssignCudaKernelExportNames",
) { generationState, irModule ->
    val context = generationState.context
    val exportForCppRuntime = context.symbols.exportForCppRuntime.owner
    irModule.files
            .filter { it.hasAnnotation(KonanFqNames.cudaCompile) }
            .flatMap { file -> file.declarations.filterIsInstance<IrSimpleFunction>().map { file to it } }
            .filter { (_, fn) -> fn.parent is IrFile && fn.visibility.isPublicAPI }
            .filter { (_, fn) -> !fn.hasAnnotation(RuntimeNames.exportForCppRuntime) }
            .forEach { (file, fn) ->
                val exportName = buildExportName(file.packageFqName.asString(), fn.name.asString())
                fn.annotations = fn.annotations + buildSimpleAnnotation(
                        context.irBuiltIns, fn.startOffset, fn.endOffset, exportForCppRuntime, exportName,
                )
            }
}

private fun buildExportName(packageFqName: String, functionName: String): String {
    val packagePart = packageFqName.replace('.', '_')
    return if (packagePart.isEmpty()) functionName else "${packagePart}__${functionName}"
}

private fun collectCrossModuleCallees(fn: IrSimpleFunction): Set<IrFunction> {
    val result = mutableSetOf<IrFunction>()
    fn.body?.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            val callee = expression.symbol.owner
            if (expression.isVirtualCall) return
            if (callee.konanLibrary == null || callee.body == null) return
            if (callee.isBoxOrUnbox()) return
            if (callee.isLazyStaticInitializer || callee.isEagerStaticInitializer) return
            if (callee.originalConstructor?.constructedClass?.isInlined() == true) return
            result.add(callee)
        }
    })
    return result
}
