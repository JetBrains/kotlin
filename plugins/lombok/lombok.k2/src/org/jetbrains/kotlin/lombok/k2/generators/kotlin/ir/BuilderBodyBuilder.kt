/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators.kotlin.ir

import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.lombok.k2.generators.BuilderGeneratorKey

class BuilderBodyBuilder : IrBodyBuilder<BuilderGeneratorKey>() {
    override fun IrBlockBodyBuilder.build(
        key: BuilderGeneratorKey,
        declaration: IrSimpleFunction,
    ) {
        TODO("Not yet implemented")
    }
}
