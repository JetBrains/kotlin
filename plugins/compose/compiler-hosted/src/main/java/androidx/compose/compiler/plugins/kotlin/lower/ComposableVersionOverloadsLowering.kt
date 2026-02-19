/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.VersionOverloadsLowering
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isAnnotation

class ComposableVersionOverloadsLowering(irFactory: IrFactory, irBuiltIns: IrBuiltIns) : VersionOverloadsLowering(irFactory, irBuiltIns) {
    constructor(context: IrPluginContext) : this(context.irFactory, context.irBuiltIns)

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.hasComposableAnnotation()) {
            super.visitFunction(declaration)
            declaration.removeVersionOverloadsAnnotations()
        }
        return declaration
    }

    override fun generateWrapper(original: IrFunction, version: MavenComparableVersion?, includedParams: BooleanArray): IrFunction =
        super.generateWrapper(original, version, includedParams).also { it.removeVersionOverloadsAnnotations() }

    // we need to remove the @IntroduceAt annotations in @Composable functions,
    // otherwise the lowering runs twice and produces wrong results
    private fun IrFunction.removeVersionOverloadsAnnotations() {
        for (parameter in parameters) {
            parameter.annotations = parameter.annotations.filter {
                !it.isAnnotation(StandardNames.FqNames.introducedAt)
            }
        }
    }
}