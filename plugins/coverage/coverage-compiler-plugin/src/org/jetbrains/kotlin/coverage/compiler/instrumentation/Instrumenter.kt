/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.compiler.instrumentation

import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.coverage.compiler.hit.HitRegistrar
import org.jetbrains.kotlin.coverage.compiler.metadata.FunctionIM
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal sealed interface Instrumenter {
    fun instrument(
        irFunction: IrFunction,
        functionIM: FunctionIM,
        irFile: IrFile,
        hitRegistrar: HitRegistrar,
        context: KotlinCoverageInstrumentationContext,
    )
}

