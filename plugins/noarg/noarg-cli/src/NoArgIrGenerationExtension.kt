/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.SYNTHESIZED_INIT_BLOCK
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class NoArgIrGenerationExtension(
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

        if (declaration.kind == ClassKind.CLASS &&
            declaration.isAnnotatedWithNoarg() &&
            declaration.constructors.none(::isZeroParameterConstructor)
        ) {
            declaration.declarations.add(getOrGenerateNoArgConstructor(declaration))
        }
    }

    private val noArgConstructors = mutableMapOf<IrClass, IrConstructor>()

    private fun getOrGenerateNoArgConstructor(klass: IrClass): IrConstructor = noArgConstructors.getOrPut(klass) {
        val superClass =
            klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS }
                ?: context.irBuiltIns.anyClass.owner

        val superConstructor =
            if (superClass.isAnnotatedWithNoarg())
                getOrGenerateNoArgConstructor(superClass)
            else superClass.constructors.singleOrNull { it.valueParameters.isEmpty() }
                ?: error("No noarg super constructor for ${klass.render()}:\n" + superClass.constructors.joinToString("\n") { it.render() })

        context.irFactory.buildConstructor {
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
                    if (invokeInitializers)
                        NoArgInitializersLowering(context.irBuiltIns).createInitializersBlock(ctor)
                    else null
                )
            )
        }
    }

    private fun IrClass.isAnnotatedWithNoarg(): Boolean =
        toIrBasedDescriptor().hasSpecialAnnotation(null)

    private fun isZeroParameterConstructor(constructor: IrConstructor): Boolean {
        val parameters = constructor.valueParameters
        return parameters.isEmpty() ||
                (parameters.all { it.defaultValue != null } && (constructor.isPrimary || constructor.hasAnnotation(JVM_OVERLOADS_FQ_NAME)))
    }
}

/** Main parts copied from [org.jetbrains.kotlin.backend.common.lower.InitializersLowering]. */
private class NoArgInitializersLowering(private val builtIns: IrBuiltIns) {
    fun createInitializersBlock(ctor: IrConstructor): IrBlock {
        val irClass = ctor.constructedClass
        return IrBlockImpl(irClass.startOffset, irClass.endOffset, builtIns.unitType, null, extractInitializers(irClass))
            .deepCopyWithSymbols(ctor)
    }

    private fun extractInitializers(irClass: IrClass): List<IrStatement> {
        val result = mutableListOf<IrStatement>()
        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrAnonymousInitializer -> if (!declaration.isStatic) {
                    result.add(with(declaration) {
                        IrBlockImpl(startOffset, endOffset, builtIns.unitType, SYNTHESIZED_INIT_BLOCK, body.statements)
                    })
                }
                is IrProperty -> declaration.backingField.let { field ->
                    if (field != null && !field.isStatic) {
                        val initializer = field.initializer
                        // Take all field initializers except those for properties in the primary constructor, for which we have no values
                        // in the noarg constructor (and thus those properties will be uninitialized).
                        if (initializer != null && initializer.expression.safeAs<IrGetValue>()?.origin !=
                            IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                        ) {
                            result.add(with(initializer) {
                                IrSetFieldImpl(
                                    startOffset, endOffset, field.symbol,
                                    IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol),
                                    expression, builtIns.unitType, IrStatementOrigin.INITIALIZE_FIELD
                                )
                            })
                        }
                    }
                }
            }
        }
        return result
    }
}
