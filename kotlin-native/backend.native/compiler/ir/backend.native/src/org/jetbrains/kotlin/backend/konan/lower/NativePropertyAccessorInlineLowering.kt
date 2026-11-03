/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.isTopLevel

internal class NativePropertyAccessorInlineLowering(private val generationState: NativeGenerationState) : PropertyAccessorInlineLowering(generationState) {
    override fun isSimpleGetter(callee: IrSimpleFunction): Boolean {
        if (generationState.context.moduleDeserializerProvider.getDeserializerOrNull(callee) != null) {
            if (callee.isTopLevel || callee.isStatic) {
                return false
            }
            // TODO: Monolithic opt treats this getters as non-trivial (they have a reinterpret call);
            //       Cached opt thinks they are trivial when building a cache (it's just a get field).
            //       Why is it so?
            callee.fqNameWhenAvailable?.asString()?.let { fqName ->
                when (fqName) {
                    "kotlinx.cinterop.CPointer.<get-value>",
                    "kotlinx.cinterop.NativePointed.<get-rawPtr>" -> return false
                }
            }
        }
        return generationState.context.isTrivialGetter(callee)
    }

    override fun isSimpleSetter(callee: IrSimpleFunction): Boolean {
        if (generationState.context.moduleDeserializerProvider.getDeserializerOrNull(callee) != null) {
            if (callee.isTopLevel || callee.isStatic) {
                return false
            }
        }
        return generationState.context.isTrivialSetter(callee)
    }
}