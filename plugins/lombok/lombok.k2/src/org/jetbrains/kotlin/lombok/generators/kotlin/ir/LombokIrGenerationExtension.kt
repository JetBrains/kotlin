/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.lombok.generators.EqualsAndHashCodeGeneratorKey
import org.jetbrains.kotlin.lombok.generators.LombokDeclarationKey
import org.jetbrains.kotlin.lombok.generators.ToStringGeneratorKey
import kotlin.reflect.KClass

class LombokIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptChildrenVoid(IrBodyBuilderVisitor(pluginContext))
    }
}

class IrBodyBuilderVisitor(private val context: IrPluginContext) : IrVisitorVoid() {
    private val bodyBuilders: Map<KClass<out LombokDeclarationKey>, IrBodyBuilder<out LombokDeclarationKey>> = mapOf(
        ToStringGeneratorKey::class to ToStringBodyBuilder,
        EqualsAndHashCodeGeneratorKey::class to EqualsAndHashCodeIrBodyBuilder,
    )

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val generatorKey = (declaration.origin as? GeneratedByPlugin)?.pluginKey
        val bodyBuilder = generatorKey?.let { bodyBuilders[it::class] }

        if (bodyBuilder != null) {
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                when (bodyBuilder) {
                    is ToStringBodyBuilder -> {
                        with(bodyBuilder) {
                            build(generatorKey as ToStringGeneratorKey, declaration)
                        }
                    }
                    is EqualsAndHashCodeIrBodyBuilder -> {
                        with(bodyBuilder) {
                            build(generatorKey as EqualsAndHashCodeGeneratorKey, declaration)
                        }
                    }
                }
            }
        } else {
            declaration.acceptChildrenVoid(this)
        }
    }
}

sealed class IrBodyBuilder<T : GeneratedDeclarationKey> {
    abstract fun IrBlockBodyBuilder.build(key: T, declaration: IrSimpleFunction)
}
