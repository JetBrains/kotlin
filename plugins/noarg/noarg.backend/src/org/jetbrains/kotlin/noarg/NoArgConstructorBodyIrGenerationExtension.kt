/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.KtModifierListOwner

class NoArgConstructorBodyIrGenerationExtension(
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptVoid(NoArgConstructorBodyIrGenerationTransformer(pluginContext, annotations, invokeInitializers))
    }
}

private val NO_ARG_CONSTRUCTOR_ORIGIN: IrDeclarationOrigin.GeneratedByPlugin = IrDeclarationOrigin.GeneratedByPlugin(NoArgPluginKey)

private class NoArgConstructorBodyIrGenerationTransformer(
    private val context: IrPluginContext,
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : IrVisitorVoid(), AnnotationBasedExtension {

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        if (declaration.origin != NO_ARG_CONSTRUCTOR_ORIGIN) return

        val klass = declaration.parent as? IrClass ?: return

        val superClass =
            klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS }
                ?: context.irBuiltIns.anyClass.owner

        val superConstructor = superClass.constructors.singleOrNull { it.isZeroParameterConstructor() }
            ?: error("No noarg super constructor for ${klass.render()}:\n" + superClass.constructors.joinToString("\n") { it.render() })

        context.generateNoArgConstructorBody(declaration, klass, superConstructor, invokeInitializers)
    }

    override fun visitBody(body: IrBody) {
        // Applying NoArg plugin to local declarations is not supported and produces an error,
        // so there is no need to go into bodies on the backend
    }
}
