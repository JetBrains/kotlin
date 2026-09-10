/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirTypeOperatorCallChecker
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * There are two casts supported by the atomicfu compiler plugin:
 * - AtomicRef<T> as AtomicRef<R>
 * - AtomicArray<T> as AtomicArray<R>
 * where T and R are some types.
 *
 * All other type operators on atomic properties are either makes no sense or could not be reasonably implemented.
 * To avoid cryptic errors from the plugin's backend, this checker reports all unsupported type operations.
 */
object AtomicfuTypeOperatorChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        val sourceAtomicKind = expression.argumentList.arguments.singleOrNull()?.resolvedType?.extractAtomicKind() ?: return
        val targetAtomicKind = expression.conversionTypeRef.coneType.extractAtomicKind()
        if (sourceAtomicKind == AtomicKind.NOT_ATOMIC && targetAtomicKind == AtomicKind.NOT_ATOMIC) return

        val isRef2RefCast = sourceAtomicKind == AtomicKind.ATOMIC_REF && targetAtomicKind == AtomicKind.ATOMIC_REF
        val isArray2ArrayCast = sourceAtomicKind == AtomicKind.ATOMIC_ARRAY && targetAtomicKind == AtomicKind.ATOMIC_ARRAY
        val isLegalCast = expression.operation == FirOperation.AS && (isRef2RefCast || isArray2ArrayCast)

        if (!isLegalCast) {
            reporter.reportOn(expression.source, AtomicfuErrors.ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN)
        }
    }
}

context(context: CheckerContext)
private fun ConeKotlinType.extractAtomicKind(): AtomicKind {
    return when (val type = fullyExpandedType()) {
        is ConeFlexibleType -> {
            val kind = type.lowerBound.extractAtomicKind()
            if (!type.isTrivial) {
                kind.join(type.upperBound.extractAtomicKind())
            } else {
                kind
            }
        }
        is ConeDefinitelyNotNullType -> type.original.extractAtomicKind()
        is ConeIntersectionType -> type.intersectedTypes.fold(AtomicKind.NOT_ATOMIC) {
            acc, t -> acc.join(t.extractAtomicKind())
        }
        else -> type.toAtomicKind()
    }
}

private fun ConeKotlinType.toAtomicKind(): AtomicKind {
    val classId = classId
    return when {
        classId?.isAtomicRefType() == true -> AtomicKind.ATOMIC_REF
        classId?.isAtomicRefArrayType() == true -> AtomicKind.ATOMIC_ARRAY
        classId?.isAtomicType() == true -> AtomicKind.ANY_ATOMIC
        else -> AtomicKind.NOT_ATOMIC
    }
}

//  NOT_ATOMIC
//  /   |   \
//  v   |   v
// REF  |  ARRAY
//    \ | /
//      v
//  ANY_ATOMIC
private enum class AtomicKind {
    ATOMIC_REF {
        override fun join(other: AtomicKind): AtomicKind = when (other) {
            // we can't handle casts from arbitrary atomic types, so join w/ any atomic results in any atomic
            ANY_ATOMIC -> ANY_ATOMIC
            // if something is both an atomic array and atomic ref,
            // then its unclear how to handle it in backend,
            // so let's consider it just an atomic and report an error
            ATOMIC_ARRAY -> ANY_ATOMIC
            // not atomic V ref -> ref, ref V ref -> ref
            else -> ATOMIC_REF
        }
    },
    ATOMIC_ARRAY {
        override fun join(other: AtomicKind): AtomicKind = when (other) {
            // we can't handle casts from arbitrary atomic types, so join w/ any atomic results in any atomic
            ANY_ATOMIC -> ANY_ATOMIC
            // if something is both an atomic array and atomic ref,
            // then its unclear how to handle it in backend,
            // so let's consider it just an atomic and report an error
            ATOMIC_REF -> ANY_ATOMIC
            // not atomic V array -> array, array V array -> array
            else -> ATOMIC_ARRAY
        }
    },
    ANY_ATOMIC {
        // we can't handle casts from/to arbitrary atomic types, it's the bottom
        override fun join(other: AtomicKind): AtomicKind = ANY_ATOMIC
    },
    NOT_ATOMIC {
        // Any other kind means checker should be concerned, so we simply update to other
        override fun join(other: AtomicKind): AtomicKind = other
    };

    abstract fun join(other: AtomicKind): AtomicKind
}
