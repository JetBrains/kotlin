/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InternalAbi
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class ExportCachesAbiVisitor(val context: Context) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)

        if (declaration.isLocal) return

        if (declaration.isCompanion) {
            val function = context.irFactory.buildFun {
                name = InternalAbi.getCompanionObjectAccessorName(declaration)
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                returnType = declaration.defaultType
            }
            context.createIrBuilder(function.symbol).apply {
                function.body = irBlockBody {
                    +irReturn(irGetObjectValue(declaration.defaultType, declaration.symbol))
                }
            }
            context.internalAbi.declare(function, declaration.module)
        }

        if (declaration.isInner) {
            val function = context.irFactory.buildFun {
                name = InternalAbi.getInnerClassOuterThisAccessorName(declaration)
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                returnType = declaration.parentAsClass.defaultType
            }
            function.addValueParameter {
                name = Name.identifier("innerClass")
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                type = declaration.defaultType
            }

            context.createIrBuilder(function.symbol).apply {
                function.body = irBlockBody {
                    +irReturn(irGetField(
                            irGet(function.valueParameters[0]),
                            this@ExportCachesAbiVisitor.context.innerClassesSupport.getOuterThisField(declaration))
                    )
                }
            }
            context.internalAbi.declare(function, declaration.module)
        }

        if (declaration.isEnumClass) {
            val function = context.irFactory.buildFun {
                name = InternalAbi.getEnumValuesAccessorName(declaration)
                returnType = context.ir.symbols.array.typeWith(declaration.defaultType)
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
            }

            context.createIrBuilder(function.symbol).run {
                function.body = irBlockBody {
                    +irReturn(with(this@ExportCachesAbiVisitor.context.enumsSupport) { irGetValuesField(declaration) })
                }
            }
            context.internalAbi.declare(function, declaration.module)
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.acceptChildrenVoid(this)

        if (!declaration.isLateinit || declaration.isFakeOverride
                || DescriptorVisibilities.isPrivate(declaration.visibility) || declaration.isLocal)
            return

        val backingField = declaration.backingField ?: error("Lateinit property ${declaration.render()} should have a backing field")
        val function = context.irFactory.buildFun {
            name = InternalAbi.getLateinitPropertyFieldAccessorName(declaration)
            origin = InternalAbi.INTERNAL_ABI_ORIGIN
            returnType = backingField.type
        }
        val ownerClass = declaration.parentClassOrNull
        if (ownerClass != null)
            function.addValueParameter {
                name = Name.identifier("owner")
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                type = ownerClass.defaultType
            }

        context.createIrBuilder(function.symbol).apply {
            function.body = irBlockBody {
                +irReturn(irGetField(ownerClass?.let { irGet(function.valueParameters[0]) }, backingField))
            }
        }
        context.internalAbi.declare(function, declaration.module)
    }
}

internal class ImportCachesAbiTransformer(val context: Context) : IrElementTransformerVoid() {
    private val companionObjectAccessors = mutableMapOf<IrClass, IrSimpleFunction>()
    private val outerThisAccessors = mutableMapOf<IrClass, IrSimpleFunction>()
    private val lateinitPropertyAccessors = mutableMapOf<IrProperty, IrSimpleFunction>()
    private val enumValuesAccessors = mutableMapOf<IrClass, IrSimpleFunction>()

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        expression.transformChildrenVoid(this)

        val irClass = expression.symbol.owner
        if (!irClass.isCompanion || context.llvmModuleSpecification.containsDeclaration(irClass)) {
            return expression
        }
        val parent = irClass.parentAsClass
        if (parent.isObjCClass()) {
            // Access to Obj-C metaclass is done via intrinsic.
            return expression
        }
        val accessor = companionObjectAccessors.getOrPut(irClass) {
            context.irFactory.buildFun {
                name = InternalAbi.getCompanionObjectAccessorName(irClass)
                returnType = irClass.defaultType
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                isExternal = true
            }.also {
                context.internalAbi.reference(it, irClass.module)
            }
        }
        return IrCallImpl(expression.startOffset, expression.endOffset, expression.type, accessor.symbol, accessor.typeParameters.size, accessor.valueParameters.size)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol.owner
        val irClass = field.parentClassOrNull
        val property = field.correspondingPropertySymbol?.owner

        return when {
            context.llvmModuleSpecification.containsDeclaration(field) -> expression

            irClass?.isInner == true && context.innerClassesSupport.getOuterThisField(irClass) == field -> {
                val accessor = outerThisAccessors.getOrPut(irClass) {
                    context.irFactory.buildFun {
                        name = InternalAbi.getInnerClassOuterThisAccessorName(irClass)
                        returnType = irClass.parentAsClass.defaultType
                        origin = InternalAbi.INTERNAL_ABI_ORIGIN
                        isExternal = true
                    }.also { function ->
                        context.internalAbi.reference(function, irClass.module)

                        function.addValueParameter {
                            name = Name.identifier("innerClass")
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            type = irClass.defaultType
                        }
                    }
                }
                return IrCallImpl(
                        expression.startOffset, expression.endOffset,
                        expression.type, accessor.symbol,
                        accessor.typeParameters.size, accessor.valueParameters.size
                ).apply {
                    putValueArgument(0, expression.receiver)
                }
            }

            property?.isLateinit == true -> {
                val accessor = lateinitPropertyAccessors.getOrPut(property) {
                    context.irFactory.buildFun {
                        name = InternalAbi.getLateinitPropertyFieldAccessorName(property)
                        returnType = field.type
                        origin = InternalAbi.INTERNAL_ABI_ORIGIN
                        isExternal = true
                    }.also { function ->
                        context.internalAbi.reference(function, property.module)

                        if (irClass != null)
                            function.addValueParameter {
                                name = Name.identifier("owner")
                                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                                type = irClass.defaultType
                            }
                    }
                }
                return IrCallImpl(
                        expression.startOffset, expression.endOffset,
                        expression.type, accessor.symbol,
                        accessor.typeParameters.size, accessor.valueParameters.size
                ).apply {
                    if (irClass != null)
                        putValueArgument(0, expression.receiver)
                }
            }

            field.origin == DECLARATION_ORIGIN_ENUM -> {
                val enumClass = irClass?.parentClassOrNull
                require(enumClass != null) { "Unexpected usage of enum VALUES field" }
                require(enumClass.isEnumClass) { "Expected a enum class: ${enumClass.render()}" }
                val accessor = enumValuesAccessors.getOrPut(enumClass) {
                    context.irFactory.buildFun {
                        name = InternalAbi.getEnumValuesAccessorName(enumClass)
                        returnType = context.ir.symbols.array.typeWith(enumClass.defaultType)
                        origin = InternalAbi.INTERNAL_ABI_ORIGIN
                        isExternal = true
                    }.also {
                        context.internalAbi.reference(it, enumClass.module)
                    }
                }
                return IrCallImpl(expression.startOffset, expression.endOffset, expression.type, accessor.symbol, accessor.typeParameters.size, accessor.valueParameters.size)
            }

            else -> expression
        }
    }
}