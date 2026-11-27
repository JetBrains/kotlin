/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("warnings")
package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.v2.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

/*
 * For classes annotated with @AllPropertiesConstructor and with no-arg constructor generates
 *   constructor which takes value parameters corresponding to all properties
 *
 * Parent class should be Any or class, annotated with @AllPropertiesConstructor
 */
class AllPropertiesConstructorIrGenerator(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val ANNOTATION_FQN = "AllPropertiesConstructor".fqn()
        private val GENERATED_PROPERTY_NAMES = setOf(
            "propertiesCount", "extensionProp", "genericExtensionProp", "outerTypeProp", "contextProp",
        )
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation()) {
            declaration.declarations += getOrGenerateMembersIfNeeded(declaration)
        }
        visitElement(declaration)
    }

    private val generatedMembers = mutableMapOf<IrClass, List<IrDeclaration>>()

    private fun getOrGenerateMembersIfNeeded(klass: IrClass): List<IrDeclaration> = generatedMembers.getOrPut(klass) {
        buildList {
            this += buildConstructor(klass)

            if (klass.declarations.none { it is IrFunction && it.name == Name.identifier("hasExtension") }) {
                this += context.irFactory.irFunction {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    name = Name.identifier("hasExtension")
                    returnType = context.irBuiltIns.unitType
                    parent = klass
                    syntheticDispatchReceiver(klass)
                    extensionReceiver {
                        name = "receiver".synthesizedName
                        type = context.irBuiltIns.stringType
                    }
                }.apply {
                    body = context.irFactory.createBlockBody(startOffset, endOffset, emptyList())
                    context.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(this)
                }
            }
            if (klass.declarations.none { it is IrFunction && it.name == Name.identifier("hasContext") }) {
                this += context.irFactory.irFunction {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    name = Name.identifier("hasContext")
                    returnType = context.irBuiltIns.unitType
                    parent = klass
                    syntheticDispatchReceiver(klass)
                    contextParameter {
                        name = Name.identifier("c")
                        type = context.irBuiltIns.stringType
                    }
                }.apply {
                    body = context.irFactory.createBlockBody(startOffset, endOffset, emptyList())
                    context.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(this)
                }
            }
            if (klass.declarations.none { it is IrProperty && it.name == Name.identifier("propertiesCount") }) {
                this += buildPropertiesCountProperty(klass)
            }
            if (klass.declarations.none { it is IrProperty && it.name == Name.identifier("extensionProp") }) {
                this += buildExtensionProperty(klass)
            }
            if (klass.declarations.none { it is IrProperty && it.name == Name.identifier("genericExtensionProp") }) {
                this += buildGenericExtensionProperty(klass)
            }
            if (klass.typeParameters.isNotEmpty() &&
                klass.declarations.none { it is IrProperty && it.name == Name.identifier("outerTypeProp") }
            ) {
                this += buildOuterTypeProperty(klass)
            }
            if (klass.declarations.none { it is IrProperty && it.name == Name.identifier("contextProp") }) {
                this += buildContextProperty(klass)
            }
        }
    }

    private fun buildPropertiesCountProperty(klass: IrClass): IrProperty {
        val propertiesCount = klass.properties.toList().size
        return buildPropertyWithBackingField(klass, Name.identifier("propertiesCount"), context.irBuiltIns.intType).apply {
            backingField!!.initializer = context.irFactory.createExpressionBody(
                IrConstImpl.int(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.intType, propertiesCount)
            )
        }
    }

    private fun buildPropertyWithBackingField(klass: IrClass, name: Name, type: IrType): IrProperty {
        val field: IrFieldSymbol
        val getterSymbol: IrSimpleFunctionSymbol
        val dispatch: IrValueParameterSymbol
        return context.irFactory.irProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            this.name = name
            parent = klass
            field = backingField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                this.type = type
                isFinal = true
            }
            getterSymbol = getter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                returnType = type
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                dispatch = syntheticDispatchReceiver(klass)
            }
        }.apply {
            getterSymbol.owner.body = context.irFactory.createBlockBody(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                listOf(
                    IrReturnImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                        context.irBuiltIns.nothingType,
                        getterSymbol,
                        IrGetFieldImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            field, type,
                            IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, dispatch)
                        )
                    )
                )
            )
            context.metadataDeclarationRegistrar.registerPropertyAsMetadataVisible(this)
        }
    }

    private fun buildExtensionProperty(klass: IrClass): IrProperty {
        val getterSymbol: IrSimpleFunctionSymbol
        return context.irFactory.irProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("extensionProp")
            parent = klass
            getterSymbol = getter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                returnType = context.irBuiltIns.stringType
                syntheticDispatchReceiver(klass)
                extensionReceiver {
                    name = "receiver".synthesizedName
                    type = context.irBuiltIns.stringType
                }
            }
        }.apply {
            getterSymbol.owner.apply {
                body = context.irFactory.createBlockBody(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    listOf(
                        IrReturnImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.nothingType,
                            symbol,
                            IrConstImpl.string(
                                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.stringType, "ext"
                            )
                        )
                    )
                )
            }
            context.metadataDeclarationRegistrar.registerPropertyAsMetadataVisible(this)
        }
    }

    private fun buildGenericExtensionProperty(klass: IrClass): IrProperty {
        val getterSymbol: IrSimpleFunctionSymbol
        val extReceiver: IrValueParameterSymbol
        return context.irFactory.irProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("genericExtensionProp")
            parent = klass
            getterSymbol = getter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                val typeParam = typeParameter {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    name = Name.identifier("T")
                    superTypes = listOf(context.irBuiltIns.anyNType)
                }
                val tType = typeParam.defaultType
                returnType = tType
                syntheticDispatchReceiver(klass)
                extReceiver = extensionReceiver {
                    name = "receiver".synthesizedName
                    type = tType
                }
            }
        }.apply {
            getterSymbol.owner.apply {
                body = context.irFactory.createBlockBody(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    listOf(
                        IrReturnImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.nothingType,
                            symbol,
                            IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, extReceiver)
                        )
                    )
                )
            }
            context.metadataDeclarationRegistrar.registerPropertyAsMetadataVisible(this)
        }
    }

    private fun buildOuterTypeProperty(klass: IrClass): IrProperty {
        val outerTypeParameter = klass.typeParameters[0]
        val outerType = outerTypeParameter.defaultType
        return buildPropertyWithBackingField(klass, Name.identifier("outerTypeProp"), outerType)
    }

    private fun buildContextProperty(klass: IrClass): IrProperty {
        val getterSymbol: IrSimpleFunctionSymbol
        val contextParam: IrValueParameterSymbol
        return context.irFactory.irProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("contextProp")
            parent = klass
            getterSymbol = getter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                returnType = context.irBuiltIns.stringType
                syntheticDispatchReceiver(klass)
                contextParam = contextParameter {
                    name = Name.identifier("c")
                    type = context.irBuiltIns.stringType
                }
            }
        }.apply {
            getterSymbol.owner.apply {
                body = context.irFactory.createBlockBody(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    listOf(
                        IrReturnImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.nothingType,
                            symbol,
                            IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, contextParam)
                        )
                    )
                )
            }
            context.metadataDeclarationRegistrar.registerPropertyAsMetadataVisible(this)
        }
    }

    private fun buildConstructor(klass: IrClass): IrConstructor {
        val superClass =
            klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS } ?: context.irBuiltIns.anyClass.owner

        val properties =
            klass.properties.toList()
                .filterNot { it.name.identifier in GENERATED_PROPERTY_NAMES }
                .sortedWith(Comparator.comparing { if (it.origin == IrDeclarationOrigin.FAKE_OVERRIDE) 0 else 1 })
        val overriddenProperties = properties.takeWhile { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
        val superConstructor = when {
            superClass.defaultType.isAny() -> superClass.constructors.singleOrNull { c -> c.parameters.none { it.kind == IrParameterKind.Regular } }
            else -> {
                require(superClass.hasAnnotation())
                superClass.constructors.singleOrNull { c -> c.parameters.any { it.kind == IrParameterKind.Regular } }
            }
        } ?: error("All properies constructor not found")

        return context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.defaultType
        }.also { ctor ->
            ctor.parent = klass
            ctor.parameters = properties.map { property ->
                buildValueParameter(ctor) {
                    type = property.getter!!.returnType
                    name = property.name
                    kind = IrParameterKind.Regular
                }
            }
            val superRegularParamCount = superConstructor.parameters.count { it.kind == IrParameterKind.Regular }
            ctor.body = context.irFactory.createBlockBody(
                ctor.startOffset, ctor.endOffset,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        ctor.startOffset, ctor.endOffset, context.irBuiltIns.unitType,
                        superConstructor.symbol, 0,
                    ).apply {
                        ctor.parameters.filter { it.kind == IrParameterKind.Regular }
                            .take(minOf(overriddenProperties.size, superRegularParamCount))
                            .forEachIndexed { index, parameter ->
                                arguments[index] = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, parameter.symbol)
                            }
                    }
                )
            )
            context.metadataDeclarationRegistrar.registerConstructorAsMetadataVisible(ctor)
        }
    }

    /**
     * The members are built detached and added to the class by [visitClass], so they don't get the implicit dispatch receiver
     * of `IrClass.irFunction {}`. The same as `createDispatchReceiverParameterWithClassParent()` of a member with synthetic offsets.
     */
    private fun IrFunctionBuilder.syntheticDispatchReceiver(klass: IrClass): IrValueParameterSymbol =
        dispatchReceiver {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            origin = klass.origin
            type = klass.defaultType
        }

    private fun IrClass.hasAnnotation(): Boolean {
        return annotations.hasAnnotation(ANNOTATION_FQN)
    }
}
