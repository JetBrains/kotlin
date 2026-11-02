/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

internal class NativePropertyAccessorInlineLowering(private val generationState: NativeGenerationState) : PropertyAccessorInlineLowering(generationState) {
    override fun isSimpleGetter(callee: IrSimpleFunction): Boolean {
        return generationState.context.isTrivialGetter(callee)
    }
}