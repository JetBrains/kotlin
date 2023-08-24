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
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.psi.KtModifierListOwner

class NoArgIrGenerationExtension(
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.accept(NoArgIrTransformer(pluginContext, annotations, invokeInitializers), null)
    }
}

private class NoArgIrTransformer(
    private val context: IrPluginContext,
    private val annotations: List<String>,
    private val invokeInitializers: Boolean,
) : AnnotationBasedExtension, IrElementVisitorVoid {
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
            ctor.body = context.irFactory.createBlockBody(
                ctor.startOffset, ctor.endOffset,
                listOfNotNull(
                    IrDelegatingConstructorCallImpl(
                        ctor.startOffset, ctor.endOffset, context.irBuiltIns.unitType,
                        superConstructor.symbol, 0, superConstructor.valueParameters.size
                    ),
                    IrInstanceInitializerCallImpl(
                        ctor.startOffset, ctor.endOffset, klass.symbol, context.irBuiltIns.unitType
                    ).takeIf { invokeInitializers }
                )
            )
        }
    }

    private fun needsNoargConstructor(declaration: IrClass): Boolean =
        declaration.kind == ClassKind.CLASS &&
                declaration.isAnnotatedWithNoarg() &&
                declaration.constructors.none { it.isZeroParameterConstructor() }

    private fun IrClass.isAnnotatedWithNoarg(): Boolean =
        toIrBasedDescriptor().hasSpecialAnnotation(null)

    // Returns true if this constructor is callable with no arguments by JVM rules, i.e. will have descriptor `()V`.
    private fun IrConstructor.isZeroParameterConstructor(): Boolean =
        valueParameters.all { it.defaultValue != null } && (valueParameters.isEmpty() || isPrimary || hasAnnotation(JVM_OVERLOADS_FQ_NAME))
}
