/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineMode

internal class NativeIrInliner(
        context: Context,
        inlineMode: InlineMode,
) : FunctionInlining(
        context = context,
        NativeInlineFunctionResolver(context, inlineMode),
        produceOuterThisFields = false,
)
