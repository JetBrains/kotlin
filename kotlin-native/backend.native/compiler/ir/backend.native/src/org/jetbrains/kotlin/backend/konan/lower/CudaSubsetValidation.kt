/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.isMultiFieldValueClass
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Validates that all IR in a `@CudaCompile`-annotated file conforms to the
 * Kotlin-on-GPU subset. Rejects, with compile-time errors, IR constructs whose lowering
 * would require host-runtime emission paths (object allocation -> AllocInstance,
 * exceptions -> landing pads + personality function, etc.) that don't exist in the CUDA
 * device runtime module.
 *
 * Gating: the fragment-split logic in `splitIntoFragments` already partitions
 * `@CudaCompile` files into a separate device IrModuleFragment, so this lowering only sees
 * device files in practice. The explicit `@CudaCompile` annotation check is belt-and-
 * suspenders in case the lowering is ever wired into a phase list that runs over host
 * files too.
 *
 * v0 checks:
 *   - Heap-class `IrConstructorCall`: only value classes (`isSingleFieldValueClass` or
 *     `isMultiFieldValueClass`) are allowed. Constructing a regular class would lower to
 *     `AllocInstance(typeInfo)` — a host-runtime call that doesn't exist on device.
 *   - `IrThrow`: device code has no exception infrastructure (no personality function,
 *     no landing pads, no `kotlin.Throwable`).
 *   - `IrTry`: same reason as `IrThrow`.
 *
 * Deferred to follow-ups: virtual-dispatch restriction (allow only inline/private/
 * top-level/value-class members), String operations, reflection, coroutines, captured-
 * state lambdas, stdlib symbol allowlist.
 */
internal class CudaSubsetValidation(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (!irFile.hasAnnotation(KonanFqNames.cudaCompile)) return

        irFile.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitConstructorCall(expression: IrConstructorCall) {
                super.visitConstructorCall(expression)
                val constructedClass = expression.symbol.owner.parentAsClass
                if (!constructedClass.isSingleFieldValueClass && !constructedClass.isMultiFieldValueClass) {
                    context.reportCompilationError(
                            "Constructing a heap class is not allowed in @CudaCompile code " +
                                    "(`${constructedClass.name.asString()}` is not a value class; " +
                                    "only `value class`-declared types and primitives are supported on device).",
                            irFile, expression
                    )
                }
            }

            override fun visitThrow(expression: IrThrow) {
                super.visitThrow(expression)
                context.reportCompilationError(
                        "`throw` is not allowed in @CudaCompile code — device code has no exception infrastructure.",
                        irFile, expression
                )
            }

            override fun visitTry(aTry: IrTry) {
                super.visitTry(aTry)
                context.reportCompilationError(
                        "`try`/`catch` is not allowed in @CudaCompile code — device code has no exception infrastructure.",
                        irFile, aTry
                )
            }
        })
    }
}
