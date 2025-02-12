/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.backend.konan.computeBinaryType
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.annotations.escapes
import org.jetbrains.kotlin.backend.konan.ir.annotations.pointsTo
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeRuntimeNames

internal class EscapeAnalysisChecker(
        private val context: ErrorReportingContext,
        private val symbols: KonanSymbols,
        private val irFile: IrFile,
) : IrVisitorVoid() {
    private fun reportWarning(location: IrElement, message: String) {
        context.report(CompilerMessageSeverity.STRONG_WARNING, location, irFile, message)
    }

    private fun reportNonFatalError(location: IrElement, message: String) {
        context.report(CompilerMessageSeverity.ERROR, location, irFile, message)
    }

    // From DFGBuilder.kt
    private val symbolsHandledByDFG: Set<IrSymbol> = with(symbols) {
        setOf(createUninitializedInstance, createUninitializedArray, createEmptyString, reinterpret, initInstance)
    }

    private val IrSymbol.handledByDFG: Boolean
        get() = this in symbolsHandledByDFG

    private val IntrinsicType.mustBeLowered: Boolean
        get() = when (this) {
            // From IntrinsicGenerator.kt
            IntrinsicType.ATOMIC_GET_FIELD,
            IntrinsicType.ATOMIC_SET_FIELD,
            IntrinsicType.GET_CONTINUATION,
            IntrinsicType.RETURN_IF_SUSPENDED,
            IntrinsicType.SAVE_COROUTINE_STATE,
            IntrinsicType.RESTORE_COROUTINE_STATE,
            IntrinsicType.INTEROP_BITS_TO_FLOAT,
            IntrinsicType.INTEROP_BITS_TO_DOUBLE,
            IntrinsicType.INTEROP_SIGN_EXTEND,
            IntrinsicType.INTEROP_NARROW,
            IntrinsicType.INTEROP_STATIC_C_FUNCTION,
            IntrinsicType.INTEROP_FUNPTR_INVOKE,
            IntrinsicType.INTEROP_CONVERT,
            IntrinsicType.ENUM_VALUES,
            IntrinsicType.ENUM_VALUE_OF,
            IntrinsicType.ENUM_ENTRIES,
            IntrinsicType.WORKER_EXECUTE,
            IntrinsicType.COMPARE_AND_SET_FIELD,
            IntrinsicType.COMPARE_AND_EXCHANGE_FIELD,
            IntrinsicType.GET_AND_SET_FIELD,
            IntrinsicType.GET_AND_ADD_FIELD -> true
            else -> false
        }

    private val FqName.isSupportedPackageByEA: Boolean
        // From EscapeAnalysis.kt
        get() = startsWith(kotlinPackageFqn)
                && !startsWith(kotlinPackageFqn.child(Name.identifier("concurrent")))
                && !startsWith(kotlinPackageFqn.child(Name.identifier("native")).child(Name.identifier("concurrent")))

    private val IrFunction.isLoweredIntrinsic: Boolean
        get() = tryGetIntrinsicType(this)?.mustBeLowered == true

    private val IrType.cannotEscape: Boolean
        get() = isUnit() || isNothing() || computeBinaryType() is BinaryType.Primitive

    private val IrType.mustEscape: Boolean
        get() = this.getClass()?.annotations?.hasAnnotation(NativeRuntimeNames.Annotations.HasFinalizer) ?: false

    private val returnValueName = "<return>"

    private data class FunctionSignatureElement(val name: String, val type: IrType)

    private val IrFunction.signatureElements: List<FunctionSignatureElement>
        get() = buildList {
            allParameters.mapTo(this) {
                FunctionSignatureElement(it.name.asString(), it.type)
            }
            add(FunctionSignatureElement(returnValueName, returnType))
        }

    private fun checkEscapesAnnotation(declaration: IrFunction, signatureElements: List<FunctionSignatureElement>) {
        val escapesName = NativeRuntimeNames.Annotations.Escapes.asFqNameString()
        val value = try {
            declaration.escapes
        } catch (e: IllegalArgumentException) {
            reportNonFatalError(declaration, "@$escapesName value is invalid: ${e.message}")
            return
        } ?: return
        signatureElements.forEachIndexed { index, element ->
            if (value.escapesAt(index)) {
                if (element.type.cannotEscape) {
                    reportNonFatalError(declaration, "${element.name} is marked as escaping by @$escapesName, but the type cannot escape to the heap")
                }
            } else {
                if (element.type.mustEscape) {
                    reportNonFatalError(declaration, "${element.name} is not marked as escaping by @$escapesName, but the type must always escape to the heap")
                }
            }
        }
    }

    private fun checkPointsToAnnotation(declaration: IrFunction, signatureElements: List<FunctionSignatureElement>) {
        val pointsToName = NativeRuntimeNames.Annotations.PointsTo.asFqNameString()
        val annotation = try {
            declaration.pointsTo
        } catch (e: IllegalArgumentException) {
            reportNonFatalError(declaration, "@$pointsToName value is invalid: ${e.message}")
            return
        } ?: return
        for (indexFrom in signatureElements.indices) {
            val from = signatureElements[indexFrom]
            for (indexTo in signatureElements.indices) {
                val to = signatureElements[indexTo]
                val kind = try {
                    annotation.kind(indexFrom, indexTo)
                } catch (e: IllegalArgumentException) {
                    reportNonFatalError(declaration, "@$pointsToName value $annotation is invalid at index $indexFrom nibble $indexTo: ${e.message}")
                    null
                } ?: continue
                if (kind.sourceIsDirect && kind.destinationIsDirect) {
                    if (from.name != returnValueName) {
                        reportNonFatalError(declaration, "${from.name} is marked as pointing to ${to.name} by @$pointsToName with kind 1, but kind 1 is only allowed for the return value")
                    }
                }
                if (from.type.cannotEscape) {
                    reportNonFatalError(declaration, "${from.name} is marked as pointing to ${to.name} by @$pointsToName, but ${from.name}'s type cannot escape to the heap")
                    break // can just go on to next `indexFrom`, no need to fail at every `indexTo`
                }
                if (to.type.cannotEscape) {
                    reportNonFatalError(declaration, "${from.name} is marked as pointing to ${to.name} by @$pointsToName, but ${to.name}'s type cannot escape to the heap")
                    continue
                }
            }
        }
    }

    private fun checkEscapeAnalysisAnnotations(declaration: IrFunction) {
        val hasEscapes = declaration.annotations.hasAnnotation(NativeRuntimeNames.Annotations.Escapes)
        val escapesName = NativeRuntimeNames.Annotations.Escapes.asFqNameString()
        val hasEscapesNothing = declaration.annotations.hasAnnotation(NativeRuntimeNames.Annotations.EscapesNothing)
        val escapesNothingName = NativeRuntimeNames.Annotations.EscapesNothing.asFqNameString()
        val hasPointsTo = declaration.annotations.hasAnnotation(NativeRuntimeNames.Annotations.PointsTo)
        val pointsToName = NativeRuntimeNames.Annotations.PointsTo.asFqNameString()

        fun warnUnusedIf(condition: Boolean, message: () -> String): Any? {
            if (!condition)
                return Unit
            if (hasEscapes) {
                reportWarning(declaration, "Unused @$escapesName: ${message()}")
            }
            if (hasEscapesNothing) {
                reportWarning(declaration, "Unused @$escapesNothingName: ${message()}")
            }
            if (hasPointsTo) {
                reportWarning(declaration, "Unused @$pointsToName: ${message()}")
            }
            // condition satisfied, potential unused warning emitted, no need to go on with other checks
            return null
        }

        warnUnusedIf(!declaration.isExternal) { "non-external function" } ?: return
        warnUnusedIf(!declaration.parent.fqNameForIrSerialization.isSupportedPackageByEA) { "package outside EA annotation checks" } ?: return
        warnUnusedIf(declaration.symbol.handledByDFG) { "function handled manually in DFGBuilder" } ?: return
        warnUnusedIf(declaration.isLoweredIntrinsic) { "function is lowered in the compiler" } ?: return

        val signatureElements = declaration.signatureElements

        warnUnusedIf(signatureElements.all { it.type.cannotEscape }) { "all of function parameters, receivers and the return value types cannot escape to the heap" } ?: return

        // All the unused checks have passed.
        // This also means, that we now know the declaration is external, in the correct package and so on.
        if (hasEscapes && hasEscapesNothing) {
            reportNonFatalError(declaration, "Conflicting @$escapesName and @$escapesNothingName")
        }
        if (!hasEscapes && !hasEscapesNothing && !hasPointsTo) {
            reportNonFatalError(declaration, "External function with parameters that may escape requires @$escapesName or @$escapesNothingName or @$pointsToName")
        }
        if (!hasEscapes && signatureElements.any { it.type.mustEscape }) {
            reportNonFatalError(declaration, "External function with parameters that must always escape is not marked by @$escapesName")
        }

        checkEscapesAnnotation(declaration, signatureElements)
        checkPointsToAnnotation(declaration, signatureElements)
    }

    override fun visitFunction(declaration: IrFunction) {
        checkEscapeAnalysisAnnotations(declaration)
        super.visitFunction(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}