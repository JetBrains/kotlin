/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("warnings")
package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.plugin.generators.AllPropertiesConstructorMetadataProvider
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.JvmNames
import java.util.Comparator

/*
 * For classes annotated with @AllPropertiesConstructor and with no-arg constructor generates
 *   constructor which takes value parameters corresponding to all properties
 *
 * Parent class should be Any or class, annotated with @AllPropertiesConstructor
 */
class AllPropertiesConstructorIrGenerator(val context: IrPluginContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation()) {
            declaration.declarations += getOrGenerateConstructorIfNeeded(declaration)
        }
        visitElement(declaration)
    }

    private val generatedConstructors = mutableMapOf<IrClass, IrConstructor>()

    private fun getOrGenerateConstructorIfNeeded(klass: IrClass): IrConstructor = generatedConstructors.getOrPut(klass) {
        val superClass = klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS } ?: context.irBuiltIns.anyClass.owner

        val properties = klass.properties.toList().sortedWith(Comparator.comparing { if (it.origin == IrDeclarationOrigin.FAKE_OVERRIDE) 0 else 1 })
        val overriddenProperties = properties.takeWhile { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
        val superConstructor = when {
            superClass.defaultType.isAny() -> superClass.constructors.singleOrNull { it.valueParameters.isEmpty() }
            else -> {
                require(superClass.hasAnnotation())
                superClass.constructors.singleOrNull { it.valueParameters.isNotEmpty() }
            }
        } ?: error("All properies constructor not found")
        Unit
        context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.defaultType
        }.also { ctor ->
            ctor.parent = klass
            ctor.valueParameters = properties.mapIndexed { index, property ->
                buildValueParameter(ctor) {
                    this.index = index
                    type = property.getter!!.returnType
                    name = property.name
                }
            }
            ctor.body = context.irFactory.createBlockBody(
                ctor.startOffset, ctor.endOffset,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        ctor.startOffset, ctor.endOffset, context.irBuiltIns.unitType,
                        superConstructor.symbol, 0, superConstructor.valueParameters.size
                    ).apply {
                        ctor.valueParameters.take(overriddenProperties.size).forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, parameter.symbol)
                            )
                        }
                    }
                )
            )
        }
    }

    private fun IrClass.hasAnnotation(): Boolean {
        return annotations.findAnnotation(AllPropertiesConstructorMetadataProvider.ANNOTATION_FQN) != null
    }


}
