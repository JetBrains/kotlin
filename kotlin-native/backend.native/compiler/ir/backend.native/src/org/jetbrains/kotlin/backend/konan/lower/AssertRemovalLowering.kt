/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.KlibAssertionRemoverLowering
import org.jetbrains.kotlin.backend.common.lower.KlibAssertionWrapperLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

internal class NativeAssertionWrapperLowering(context: Context) : KlibAssertionWrapperLowering(context) {
    override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol = context.ir.symbols.isAssertionArgumentEvaluationEnabled
}

internal class NativeAssertionRemoverLowering(context: Context) : KlibAssertionRemoverLowering(
        context, context.config.assertsEnabled, context.config.assertsEnabled
) {
    override val isAssertionThrowingErrorEnabled: IrSimpleFunctionSymbol = context.ir.symbols.isAssertionThrowingErrorEnabled
    override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol = context.ir.symbols.isAssertionArgumentEvaluationEnabled
}
