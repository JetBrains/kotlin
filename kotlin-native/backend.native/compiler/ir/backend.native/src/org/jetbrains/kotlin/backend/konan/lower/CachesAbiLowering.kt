/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.allOverriddenFunctions
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import kotlin.collections.plus

private var IrClass.outerThisAccessor: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrProperty.lateinitPropertyAccessor: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrField.topLevelFieldAccessor: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrSimpleFunction.fakeOverrideAccessor: IrSimpleFunction? by irAttribute(copyByDefault = false)

private val IrSimpleFunction.isPrivateOrBelongsToPrivateClass: Boolean
    get() {
        if (DescriptorVisibilities.isPrivate(this.visibility)) return true
        val owner = this.correspondingPropertySymbol?.owner?.parent ?: this.parent
        if (owner !is IrClass) return false
        return DescriptorVisibilities.isPrivate(owner.visibility) || owner.isOriginallyLocal
    }

private val IrSimpleFunction.needsFakeOverrideAccessor: Boolean
    get() {
        if (this.isPrivateOrBelongsToPrivateClass) return false
        if (!this.isFakeOverride) return false
        return this.allOverriddenFunctions.any {
            !it.isFakeOverride && it.modality != Modality.ABSTRACT
                    && it.isPrivateOrBelongsToPrivateClass
        }
    }

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
internal class CachesAbiSupport(private val irFactory: IrFactory) {
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

    fun getFakeOverrideAccessor(irFunction: IrSimpleFunction): IrSimpleFunction {
        return irFunction::fakeOverrideAccessor.getOrSetIfNull {
            val owner = irFunction.correspondingPropertySymbol?.owner?.parent ?: irFunction.parent
            owner as? IrClass ?: error("An instance method expected: ${irFunction.render()}")
            irFactory.buildFun {
                name = getMangledNameFor("${irFunction.name}_accessor", owner)
                origin = INTERNAL_ABI_ORIGIN
            }.apply {
                parent = irFunction.getPackageFragment()
                attributeOwnerId = irFunction // To be able to get the file.

                addValueParameter {
                    name = Name.identifier("inst")
                    origin = INTERNAL_ABI_ORIGIN
                    type = owner.defaultType
                }

                typeParameters = irFunction.typeParameters.map { parameter -> parameter.copyToWithoutSuperTypes(this) }

                val typeSubstitutor = IrTypeSubstitutor(
                        irFunction.typeParameters.map { it.symbol },
                        typeParameters.map { it.defaultType },
                        allowEmptySubstitution = true
                )
                irFunction.typeParameters.forEachIndexed { index, parameter ->
                    typeParameters[index].superTypes = parameter.superTypes.map { typeSubstitutor.substitute(it) }
                }
                returnType = typeSubstitutor.substitute(irFunction.returnType)

                parameters += irFunction.nonDispatchParameters.map { parameter ->
                    parameter.copyTo(
                            this,
                            type = typeSubstitutor.substitute(parameter.type),
                            defaultValue = null,
                    )
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
internal class ExportCachesAbiVisitor(val context: Context) : FileLoweringPass {
    private val cachesAbiSupport = context.cachesAbiSupport

    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(visitor)
        irFile.addChildren(visitor.addedFunctions)
    }

    private val visitor = object : IrVisitorVoid() {
        val addedFunctions = mutableListOf<IrSimpleFunction>()
        val handledFakeOverrides = mutableSetOf<IrSimpleFunction>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            declaration.acceptChildrenVoid(this)

            if (declaration.isOriginallyLocal) return


            if (declaration.isInner) {
                val function = cachesAbiSupport.getOuterThisAccessor(declaration)
                context.createIrBuilder(function.symbol).apply {
                    function.body = irBlockBody {
                        +irReturn(irGetField(
                                irGet(function.parameters[0]),
                                this@ExportCachesAbiVisitor.context.innerClassesSupport.getOuterThisField(declaration))
                        )
                    }
                }
                addedFunctions.add(function)
            }

            if (DescriptorVisibilities.isPrivate(declaration.visibility)) return
            for (irFunction in declaration.simpleFunctions()) {
                if (!irFunction.needsFakeOverrideAccessor) continue

                // Check if the base function has default values. If it does, then at the [irFunction] call sites a special generated
                // $default function will be called, but it will be generated for the base function rather than for [irFunction].
                // So in that case an accessor should be to the base function.
                val baseFunction = irFunction.resolveFakeOverrideMaybeAbstract()
                val hasDefaultValues = baseFunction?.parameters?.any { it.defaultValue != null } == true
                val accessorTarget = if (hasDefaultValues) baseFunction else irFunction
                if (accessorTarget in handledFakeOverrides) return

                val accessor = cachesAbiSupport.getFakeOverrideAccessor(accessorTarget)
                context.createIrBuilder(accessor.symbol).apply {
                    accessor.body = irBlockBody {
                        +irReturn(
                                irCall(accessorTarget.symbol, accessor.returnType, accessor.typeParameters.map { it.defaultType })
                                        .apply {
                                            accessor.parameters.forEachIndexed { idx, param -> arguments[idx] = irGet(param) }
                                        }
                        )
                    }
                }
                handledFakeOverrides.add(accessorTarget)
                addedFunctions.add(accessor)
            }
        }

        override fun visitProperty(declaration: IrProperty) {
            declaration.acceptChildrenVoid(this)

            if (!declaration.isLateinit || declaration.isFakeOverride
                    || DescriptorVisibilities.isPrivate(declaration.visibility) || declaration.isOriginallyLocal)
                return

            val backingField = declaration.backingField ?: error("Lateinit property ${declaration.render()} should have a backing field")
            val ownerClass = declaration.parentClassOrNull
            val function = cachesAbiSupport.getLateinitPropertyAccessor(declaration)
            context.createIrBuilder(function.symbol).apply {
                function.body = irBlockBody {
                    +irReturn(irGetField(ownerClass?.let { irGet(function.parameters[0]) }, backingField))
                }
            }
            addedFunctions.add(function)
        }

        // This is workaround for KT-68797, should be dropped in KT-68916
        override fun visitField(declaration: IrField) {
            declaration.acceptChildrenVoid(this)

            if (!declaration.isTopLevel || DescriptorVisibilities.isPrivate(declaration.visibility)) return

            val getter = cachesAbiSupport.getTopLevelFieldAccessor(declaration)
            context.createIrBuilder(getter.symbol).apply {
                getter.body = irBlockBody {
                    +irReturn(irGetField(null, declaration))
                }
            }
            addedFunctions.add(getter)
        }
    }
}

internal class ImportCachesAbiTransformer(val generationState: NativeGenerationState) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(Transformer(irFile))
    }

    private inner class Transformer(val irFile: IrFile) : IrElementTransformerVoid() {
        private val cachesAbiSupport = generationState.context.cachesAbiSupport
        private val innerClassesSupport = generationState.context.innerClassesSupport
        private val dependenciesTracker = generationState.dependenciesTracker

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val callee = expression.symbol.owner
            if (!callee.needsFakeOverrideAccessor) {
                val owner = callee.parentClassOrNull ?: return expression
                val receiverType = expression.dispatchReceiver?.type?.erasedUpperBound ?: return expression
                if (!callee.isPrivateOrBelongsToPrivateClass || owner == receiverType)
                    return expression
            }
            val accessor = cachesAbiSupport.getFakeOverrideAccessor(callee)
            if (accessor.fileOrNull == irFile || generationState.llvmModuleSpecification.containsDeclaration(callee))
                return expression
            return irCall(expression, accessor)
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
                            arguments[0] = expression.receiver
                        }
                    }
                }

                property?.isLateinit == true -> {
                    val accessor = cachesAbiSupport.getLateinitPropertyAccessor(property)
                    dependenciesTracker.add(property)
                    createIrBuilder().run {
                        irCall(accessor).apply {
                            if (irClass != null)
                                arguments[0] = expression.receiver
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
}
