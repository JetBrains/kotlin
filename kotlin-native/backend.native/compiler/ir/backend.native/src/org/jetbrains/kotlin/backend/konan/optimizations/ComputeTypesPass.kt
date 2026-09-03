/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.AbstractComputeTypesPass
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.konan.getInlinedClassNative
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType

@PhasePrerequisites(FinallyBlocksLowering::class)
internal class ComputeTypesPass(context: LoweringContext) : AbstractComputeTypesPass(context) {
    override fun IrType.getInlinedClassOrNull(): IrClass? = getInlinedClassNative()
}
