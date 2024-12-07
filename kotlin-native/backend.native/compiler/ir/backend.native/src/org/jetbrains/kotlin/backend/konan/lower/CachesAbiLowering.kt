/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

private var IrClass.outerThisAccessor: IrSimpleFunction? by irAttribute(followAttributeOwner = false)
private var IrProperty.lateinitPropertyAccessor: IrSimpleFunction? by irAttribute(followAttributeOwner = false)
private var IrField.topLevelFieldAccessor: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * Allows to distinguish external declarations to internal ABI.
 */
internal val INTERNAL_ABI_ORIGIN = IrDeclarationOriginImpl("INTERNAL_ABI")

/**
 * Sometimes we need to reference symbols that are not declared in metadata.
 * For example, symbol might be declared during lowering.
 * In case of compiler caches, this means that it is not accessible as Lazy IR
 * and we have to explicitly add an external declaration.
 */
internal class CachesAbiSupport(mapping: Mapping, private val irFactory: IrFactory) {
    fun getOuterThisAccessor(irClass: IrClass): IrSimpleFunction {
        require(irClass.isInner) { "Expected an inner class but was: ${irClass.render()}" }
        return irClass::outerThisAccessor.getOrSetIfNull {
            irFactory.buildFun {
                name = getMangledNameFor("outerThis", irClass)
                origin = INTERNAL_ABI_ORIGIN
                returnType = irClass.parentAsClass.defaultType
            }.apply {
                parent = irClass.getPackageFragment()
                attributeOwnerId = irClass // To be able to get the file.

                addValueParameter {
                    name = Name.identifier("innerClass")
                    origin = INTERNAL_ABI_ORIGIN
                    type = irClass.defaultType
                }
            }
        }
    }

    // This is workaround for KT-68797, should be dropped in KT-68916
    fun getTopLevelFieldAccessor(irField: IrField): IrSimpleFunction {
        require(irField.isTopLevel)
        return irField::topLevelFieldAccessor.getOrSetIfNull {
            irFactory.buildFun {
                name = getMangledNameFor("${irField.name}_get", irField.parent)
                origin = INTERNAL_ABI_ORIGIN
                returnType = irField.type
            }.apply {
                parent = irField.parent
            }
        }
    }

    fun getLateinitPropertyAccessor(irProperty: IrProperty): IrSimpleFunction {
        require(irProperty.isLateinit) { "Expected a lateinit property but was: ${irProperty.render()}" }
        return irProperty::lateinitPropertyAccessor.getOrSetIfNull {
            val backingField = irProperty.backingField ?: error("Lateinit property ${irProperty.render()} should have a backing field")
            val owner = irProperty.parent
            irFactory.buildFun {
                name = getMangledNameFor("${irProperty.name}_field", owner)
                origin = INTERNAL_ABI_ORIGIN
                returnType = backingField.type
            }.apply {
                parent = irProperty.getPackageFragment()
                attributeOwnerId = irProperty // To be able to get the file.

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

    /**
     * Generate name for declaration that will be a part of internal ABI.
     */
    private fun getMangledNameFor(declarationName: String, parent: IrDeclarationParent): Name {
        val prefix = (parent as? IrFile)?.path ?: parent.fqNameForIrSerialization
        return "$prefix.$declarationName".synthesizedName
    }
}

/**
 * Adds accessors to private entities.
 */
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

    // This is workaround for KT-68797, should be dropped in KT-68916
    override fun visitField(declaration: IrField, data: MutableList<IrFunction>) {
        declaration.acceptChildren(this, data)

        if (!declaration.isTopLevel || DescriptorVisibilities.isPrivate(declaration.visibility)) return

        val getter = cachesAbiSupport.getTopLevelFieldAccessor(declaration)
        context.createIrBuilder(getter.symbol).apply {
            getter.body = irBlockBody {
                +irReturn(irGetField(null, declaration))
            }
        }
        data.add(getter)
    }
}

internal class ImportCachesAbiTransformer(val generationState: NativeGenerationState) : FileLoweringPass, IrElementTransformerVoid() {
    private val cachesAbiSupport = generationState.context.cachesAbiSupport
    private val innerClassesSupport = generationState.context.innerClassesSupport
    private val dependenciesTracker = generationState.dependenciesTracker

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }


    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol.owner
        val irClass = field.parentClassOrNull
        val property = field.correspondingPropertySymbol?.owner

        // Actual scope for builder is the current function that we don't have access to. So we put a new symbol as scope here,
        // but it will not affect the result because we are not creating any declarations here.
        fun createIrBuilder() = generationState.context.irBuiltIns.createIrBuilder(
                IrSimpleFunctionSymbolImpl(), expression.startOffset, expression.endOffset
        )

        return when {
            generationState.llvmModuleSpecification.containsDeclaration(field) -> expression

            irClass?.isInner == true && innerClassesSupport.getOuterThisField(irClass) == field -> {
                val accessor = cachesAbiSupport.getOuterThisAccessor(irClass)
                dependenciesTracker.add(irClass)
                createIrBuilder().run {
                    irCall(accessor).apply {
                        putValueArgument(0, expression.receiver)
                    }
                }
            }

            property?.isLateinit == true -> {
                val accessor = cachesAbiSupport.getLateinitPropertyAccessor(property)
                dependenciesTracker.add(property)
                createIrBuilder().run {
                    irCall(accessor).apply {
                        if (irClass != null)
                            putValueArgument(0, expression.receiver)
                    }
                }
            }

            field.isTopLevel -> {
                val accessor = cachesAbiSupport.getTopLevelFieldAccessor(field)
                dependenciesTracker.add(field)
                createIrBuilder().irCall(accessor)
            }

            else -> expression
        }
    }
}