/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.psi.KtModifierListOwner

class NoArgFullConstructorIrGenerationExtension(
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.accept(NoArgFullConstructorIrGenerationTransformer(pluginContext, annotations, invokeInitializers), null)
    }
}

private class NoArgFullConstructorIrGenerationTransformer(
    private val context: IrPluginContext,
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : IrVisitorVoid(), AnnotationBasedExtension {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (needsNoargConstructor(declaration)) {
            declaration.declarations.add(getOrGenerateNoArgConstructor(declaration))
        }
    }

    private val noArgConstructors = mutableMapOf<IrClass, IrConstructor>()

    private fun getOrGenerateNoArgConstructor(klass: IrClass): IrConstructor = noArgConstructors.getOrPut(klass) {
        val superClass =
            klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS }
                ?: context.irBuiltIns.anyClass.owner

        val superConstructor =
            if (needsNoargConstructor(superClass))
                getOrGenerateNoArgConstructor(superClass)
            else superClass.constructors.singleOrNull { it.isZeroParameterConstructor() }
                ?: error("No noarg super constructor for ${klass.render()}:\n" + superClass.constructors.joinToString("\n") { it.render() })

        context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.defaultType
        }.also { ctor ->
            ctor.parent = klass
            context.generateNoArgConstructorBody(ctor, klass, superConstructor, invokeInitializers)
        }
    }

    private fun needsNoargConstructor(declaration: IrClass): Boolean =
        declaration.kind == ClassKind.CLASS &&
                declaration.isAnnotatedWithNoarg() &&
                declaration.constructors.none { it.isZeroParameterConstructor() }

    private fun IrClass.isAnnotatedWithNoarg(): Boolean =
        toIrBasedDescriptor().hasSpecialAnnotation(null)
}
