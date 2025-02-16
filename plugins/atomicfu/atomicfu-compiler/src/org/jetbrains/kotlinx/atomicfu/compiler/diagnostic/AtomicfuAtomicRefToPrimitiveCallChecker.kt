/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicfuStandardClassIds

object AtomicfuAtomicRefToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    AtomicfuStandardClassIds.atomicByPrimitive,
    MppCheckerKind.Platform,
    AtomicfuStandardClassIds.Callables.atomicRefCompareAndSet,
    AtomicfuStandardClassIds.Callables.atomicRefCompareAndExchange,
)
