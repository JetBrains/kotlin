/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irCall

internal fun Context.getObjectClassInstanceFunction(clazz: IrClass) = mapping.objectInstanceGetter.getOrPut(clazz) {
    when {
        clazz.isUnit() -> ir.symbols.theUnitInstance.owner
        clazz.isCompanion -> {
            require((clazz.parent as? IrClass)?.isExternalObjCClass() != true) { "External objc Classes can't be used this way"}
            val property = irFactory.buildProperty {
                startOffset = clazz.startOffset
                endOffset = clazz.endOffset
                name = "companion".synthesizedName
            }.apply {
                parent = clazz.parent
                addGetter {
                    startOffset = clazz.startOffset
                    endOffset = clazz.endOffset
                    returnType = clazz.defaultType
                }
            }
            property.getter!!
        }
        else -> {
            val property = irFactory.buildProperty {
                startOffset = clazz.startOffset
                endOffset = clazz.endOffset
                name = "instance".synthesizedName
            }.apply {
                parent = clazz
                addGetter {
                    startOffset = clazz.startOffset
                    endOffset = clazz.endOffset
                    returnType = clazz.defaultType
                }
            }
            property.getter!!
        }
    }
}

internal class ObjectClassLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    val context = generationState.context
    val symbols = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.transform(object: IrBuildingTransformer(context) {
            override fun visitClass(declaration: IrClass) : IrDeclaration {
                super.visitClass(declaration)
                if (declaration.isObject && !declaration.isCompanion && !declaration.isUnit()) {
                    processObjectClass(
                            declaration,
                            declaration
                    )
                }
                declaration.declarations.singleOrNull { (it as? IrClass)?.isCompanion == true }?.let {
                    processObjectClass(
                            it as IrClass,
                            declaration
                    )
                }
                return declaration
            }

            override fun visitGetObjectValue(expression: IrGetObjectValue) : IrExpression {
                builder.at(expression)
                val singleton = expression.symbol.owner
                return if (singleton.isCompanion && (singleton.parent as IrClass).isExternalObjCClass())
                    builder.irGetObjCClassCompanion(singleton)
                else
                    builder.irCall(context.getObjectClassInstanceFunction(singleton))
            }
        }, null)
    }

    fun IrBuilderWithScope.irGetObjCClassCompanion(declaration: IrClass): IrExpression {
        require(declaration.isCompanion && (declaration.parent as IrClass).isObjCClass())
        return irCallWithSubstitutedType(symbols.interopInterpretObjCPointer, listOf(declaration.defaultType)).apply {
            putValueArgument(0, irCallWithSubstitutedType(symbols.interopGetObjCClass, listOf((declaration.parent as IrClass).defaultType)))
        }
    }

    fun processObjectClass(
            declaration: IrClass,
            classToAdd: IrClass
    ) {
        val function = context.getObjectClassInstanceFunction(declaration)
        val property = function.correspondingPropertySymbol!!.owner
        classToAdd.declarations.add(property)

        property.addBackingField {
            isFinal = true
            isStatic = true
            type = declaration.defaultType
            visibility = DescriptorVisibilities.PRIVATE
        }.also { field ->
            val primaryConstructor = declaration.constructors.single { it.isPrimary }
            require(primaryConstructor.valueParameters.isEmpty())
            val builder = context.createIrBuilder(field.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
            val initializer = if (declaration.isCompanion && classToAdd.isObjCClass()) {
                builder.irGetObjCClassCompanion(declaration)
            } else {
                if (declaration.hasConstStateAndNoSideEffects() && !declaration.annotations.hasAnnotation(KonanFqNames.threadLocal)) {
                    builder.irConstantObject(
                            declaration,
                            emptyList()
                    )
                } else {
                    builder.irBlock {
                        // we need to make object available for rereading from the same thread while initializing
                        val uninitializedInstanceCall = irCallWithSubstitutedType(symbols.createUninitializedInstance, listOf(declaration.defaultType))
                        +irSetField(null, field, uninitializedInstanceCall, origin = IrStatementOriginFieldPreInit)
                        +irCall(symbols.initInstance).apply {
                            putValueArgument(0, irGetField(null, field))
                            putValueArgument(1, irCallConstructor(primaryConstructor.symbol, emptyList()))
                        }
                        +irGetField(null, field)
                    }
                }
            }
            field.initializer = builder.irExprBody(initializer)
        }
        if (declaration.annotations.hasAnnotation(KonanFqNames.threadLocal)) {
            property.annotations += buildSimpleAnnotation(context.irBuiltIns, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.ir.symbols.threadLocal.owner)
        } else if (declaration.annotations.hasAnnotation(KonanFqNames.sharedImmutable) || context.memoryModel != MemoryModel.EXPERIMENTAL){
            property.annotations += buildSimpleAnnotation(context.irBuiltIns, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.ir.symbols.sharedImmutable.owner)
        }
        function.body = context.createIrBuilder(function.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +irReturn(irGetField(null, property.backingField!!))
        }
    }

    private fun IrConstructor.isAutogeneratedSimpleConstructor(): Boolean {
        if (!this.isPrimary) return false
        if (valueParameters.isNotEmpty()) return false
        val statements = this.body?.statements ?: return false
        if (statements.isEmpty()) return false
        val constructorCall = statements[0] as? IrDelegatingConstructorCall ?: return false
        val constructor = constructorCall.symbol.owner as? IrConstructor ?: return false
        if (!constructor.constructedClass.isAny()) return false
        return statements.asSequence().drop(1).all {
            it is IrBlock && it.origin == IrStatementOrigin.INITIALIZE_FIELD
        }
    }

    private fun IrClass.hasConstStateAndNoSideEffects(): Boolean {
        if (this.hasAnnotation(KonanFqNames.canBePrecreated)) return true
        val fields = context.getLayoutBuilder(this).getFields(generationState.llvm)
        return fields.all { it.isConst } && this.constructors.all { it.isAutogeneratedSimpleConstructor() }
    }


    // This is a hack to avoid early freezing. Should be removed when freezing is removed
    object IrStatementOriginFieldPreInit : IrStatementOriginImpl("FIELD_PRE_INIT")

}