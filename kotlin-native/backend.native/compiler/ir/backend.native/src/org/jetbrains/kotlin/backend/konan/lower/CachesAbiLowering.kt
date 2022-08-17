/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeMapping
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

/**
 * Allows to distinguish external declarations to internal ABI.
 */
internal object INTERNAL_ABI_ORIGIN : IrDeclarationOriginImpl("INTERNAL_ABI")

/**
 * Sometimes we need to reference symbols that are not declared in metadata.
 * For example, symbol might be declared during lowering.
 * In case of compiler caches, this means that it is not accessible as Lazy IR
 * and we have to explicitly add an external declaration.
 */
internal class CachesAbiSupport(mapping: NativeMapping, symbols: KonanSymbols, private val irFactory: IrFactory) {
    private val companionObjectAccessors = mapping.companionObjectCacheAccessors
    private val outerThisAccessors = mapping.outerThisCacheAccessors
    private val lateinitPropertyAccessors = mapping.lateinitPropertyCacheAccessors
    private val enumValuesAccessors = mapping.enumValuesCacheAccessors
    private val lateInitFieldToNullableField = mapping.lateInitFieldToNullableField
    private val array = symbols.array

    fun getCompanionObjectAccessor(irClass: IrClass): IrSimpleFunction {
        require(irClass.isCompanion) { "Expected a companion object but was: ${irClass.render()}" }
        return companionObjectAccessors.getOrPut(irClass) {
            irFactory.buildFun {
                name = getMangledNameFor("globalAccessor", irClass)
                origin = INTERNAL_ABI_ORIGIN
                returnType = irClass.defaultType
            }.apply {
                parent = irClass.getPackageFragment()
            }
        }
    }

    fun getOuterThisAccessor(irClass: IrClass): IrSimpleFunction {
        require(irClass.isInner) { "Expected an inner class but was: ${irClass.render()}" }
        return outerThisAccessors.getOrPut(irClass) {
            irFactory.buildFun {
                name = getMangledNameFor("outerThis", irClass)
                origin = INTERNAL_ABI_ORIGIN
                returnType = irClass.parentAsClass.defaultType
            }.apply {
                parent = irClass.getPackageFragment()

                addValueParameter {
                    name = Name.identifier("innerClass")
                    origin = INTERNAL_ABI_ORIGIN
                    type = irClass.defaultType
                }
            }
        }
    }

    fun getLateinitPropertyAccessor(irProperty: IrProperty): IrSimpleFunction {
        require(irProperty.isLateinit) { "Expected a lateinit property but was: ${irProperty.render()}" }
        return lateinitPropertyAccessors.getOrPut(irProperty) {
            val backingField = irProperty.backingField ?: error("Lateinit property ${irProperty.render()} should have a backing field")
            val actualField = lateInitFieldToNullableField[backingField] ?: backingField
            val owner = irProperty.parent
            irFactory.buildFun {
                name = getMangledNameFor("${irProperty.name}_field", owner)
                origin = INTERNAL_ABI_ORIGIN
                returnType = actualField.type
            }.apply {
                parent = irProperty.getPackageFragment()

                (owner as? IrClass)?.let {
                    addValueParameter {
                        name = Name.identifier("owner")
                        origin = INTERNAL_ABI_ORIGIN
                        type = it.defaultType
                    }
                }
            }
        }
    }

    fun getEnumValuesAccessor(irClass: IrClass): IrSimpleFunction {
        require(irClass.isEnumClass) { "Expected a enum class but was: ${irClass.render()}" }
        return enumValuesAccessors.getOrPut(irClass) {
            irFactory.buildFun {
                name = getMangledNameFor("getValues", irClass)
                returnType = array.typeWith(irClass.defaultType)
                origin = INTERNAL_ABI_ORIGIN
            }.apply {
                parent = irClass.getPackageFragment()
            }
        }
    }

    /**
     * Generate name for declaration that will be a part of internal ABI.
     */
    private fun getMangledNameFor(declarationName: String, parent: IrDeclarationParent): Name {
        val prefix = parent.fqNameForIrSerialization
        return "$prefix.$declarationName".synthesizedName
    }
}

internal class ExportCachesAbiVisitor(val context: Context) : FileLoweringPass, IrElementVisitor<Unit, MutableList<IrFunction>> {
    private val cachesAbiSupport = context.cachesAbiSupport

    override fun lower(irFile: IrFile) {
        val addedFunctions = mutableListOf<IrFunction>()
        irFile.acceptChildren(this, addedFunctions)
        irFile.addChildren(addedFunctions)
    }

    override fun visitElement(element: IrElement, data: MutableList<IrFunction>) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: MutableList<IrFunction>) {
        declaration.acceptChildren(this, data)

        if (declaration.isLocal) return

        if (declaration.isCompanion) {
            val function = cachesAbiSupport.getCompanionObjectAccessor(declaration)
            context.createIrBuilder(function.symbol).apply {
                function.body = irBlockBody {
                    +irReturn(irGetObjectValue(declaration.defaultType, declaration.symbol))
                }
            }
            data.add(function)
        }

        if (declaration.isInner) {
            val function = cachesAbiSupport.getOuterThisAccessor(declaration)
            context.createIrBuilder(function.symbol).apply {
                function.body = irBlockBody {
                    +irReturn(irGetField(
                            irGet(function.valueParameters[0]),
                            this@ExportCachesAbiVisitor.context.innerClassesSupport.getOuterThisField(declaration))
                    )
                }
            }
            data.add(function)
        }

        if (declaration.isEnumClass) {
            val function = cachesAbiSupport.getEnumValuesAccessor(declaration)
            context.createIrBuilder(function.symbol).run {
                function.body = irBlockBody {
                    +irReturn(with(this@ExportCachesAbiVisitor.context.enumsSupport) { irGetValuesField(declaration) })
                }
            }
            data.add(function)
        }
    }

    override fun visitProperty(declaration: IrProperty, data: MutableList<IrFunction>) {
        declaration.acceptChildren(this, data)

        if (!declaration.isLateinit || declaration.isFakeOverride
                || DescriptorVisibilities.isPrivate(declaration.visibility) || declaration.isLocal)
            return

        val backingField = declaration.backingField ?: error("Lateinit property ${declaration.render()} should have a backing field")
        val ownerClass = declaration.parentClassOrNull
        val function = cachesAbiSupport.getLateinitPropertyAccessor(declaration)
        context.createIrBuilder(function.symbol).apply {
            function.body = irBlockBody {
                +irReturn(irGetField(ownerClass?.let { irGet(function.valueParameters[0]) }, backingField))
            }
        }
        data.add(function)
    }
}

internal class ImportCachesAbiTransformer(val context: Context) : FileLoweringPass, IrElementTransformerVoid() {
    private val cachesAbiSupport = context.cachesAbiSupport
    private val enumsSupport = context.enumsSupport
    private val llvmImports = context.generationState.llvmImports

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

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
        val accessor = cachesAbiSupport.getCompanionObjectAccessor(irClass)
        llvmImports.add(irClass.llvmSymbolOrigin)
        return irCall(expression.startOffset, expression.endOffset, accessor, emptyList())
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol.owner
        val irClass = field.parentClassOrNull
        val property = field.correspondingPropertySymbol?.owner

        return when {
            context.llvmModuleSpecification.containsDeclaration(field) -> expression

            irClass?.isInner == true && context.innerClassesSupport.getOuterThisField(irClass) == field -> {
                val accessor = cachesAbiSupport.getOuterThisAccessor(irClass)
                llvmImports.add(irClass.llvmSymbolOrigin)
                return irCall(expression.startOffset, expression.endOffset, accessor, emptyList()).apply {
                    putValueArgument(0, expression.receiver)
                }
            }

            property?.isLateinit == true -> {
                val accessor = cachesAbiSupport.getLateinitPropertyAccessor(property)
                llvmImports.add(property.llvmSymbolOrigin)
                return irCall(expression.startOffset, expression.endOffset, accessor, emptyList()).apply {
                    if (irClass != null)
                        putValueArgument(0, expression.receiver)
                }
            }

            field.origin == DECLARATION_ORIGIN_ENUM -> {
                val enumClass = irClass?.parentClassOrNull
                require(enumClass != null) { "Unexpected usage of enum VALUES field" }
                require(enumClass.isEnumClass) { "Expected a enum class: ${enumClass.render()}" }
                require(enumsSupport.getImplObject(enumClass) == irClass) { "Expected a enum's impl object: ${irClass.render()}" }
                require(field == enumsSupport.getValuesField(irClass)) { "Expected VALUES field: ${field.render()}" }
                val accessor = cachesAbiSupport.getEnumValuesAccessor(enumClass)
                llvmImports.add(enumClass.llvmSymbolOrigin)
                return irCall(expression.startOffset, expression.endOffset, accessor, emptyList())
            }

            else -> expression
        }
    }
}