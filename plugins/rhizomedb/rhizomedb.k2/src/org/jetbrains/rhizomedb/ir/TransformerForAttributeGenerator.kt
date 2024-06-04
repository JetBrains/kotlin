/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.rhizomedb.fir.*
import org.jetbrains.rhizomedb.fir.checkers.AttributeAnnotationKind
import org.jetbrains.rhizomedb.ir.CallKind.*
import org.jetbrains.rhizomedb.ir.serializers.CompilerPluginContext
import org.jetbrains.rhizomedb.ir.serializers.generateSerializerCall

class TransformerForAttributeGenerator(private val pluginContext: IrPluginContext, private val myContext: CompilerPluginContext) : IrElementVisitorVoid {

    private val irBuiltIns = pluginContext.irBuiltIns

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        if (origin !is GeneratedByPlugin || origin.pluginKey !is RhizomedbAttributePluginKey) {
            visitElement(declaration)
            return
        }
        val getter = declaration.getter ?: declaration.addGetter()

        val ownerProperty = declaration.parentClassOrNull?.parentClassOrNull?.declarations?.filterIsInstance<IrProperty>()?.firstOrNull {
            it.name == declaration.name.toPropertyName()
        } ?: error("NoProperty???")

        val typeT = (declaration.getter!!.returnType as IrSimpleType).arguments[0].typeOrFail
        val attributeKind = (origin.pluginKey as RhizomedbAttributePluginKey).kind
        val (annotation, kind) = ownerProperty.attributeAnnotation() ?: error("No attribute annotation found")
        val defaultValueType = irBuiltIns.defaultValueType.typeWith(typeT).withNullability(true)
        val flags = annotation.getValueArgument(0)
        val call = irBuiltIns.createIrBuilder(declaration.symbol).run {
            when (kind.join(attributeKind)) {
                // fun <T : Any> requiredValue(
                //    name: String,
                //    serializer: KSerializer<T>,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //    defaultValueProvider: DefaultValue<T>? = null
                //  )
                REQUIRED_VALUE -> attributeIrCall(declaration, RhizomedbSymbolNames.requiredValueName) {
                    val serializer = generateSerializerCall(typeT, myContext, "???")
                    putValueArgument(1, serializer)
                    putValueArgument(2, flags ?: irNotIndexedEntry())
                    putValueArgument(3, irNull(defaultValueType))
                }
                // fun <T : Any> optionalValue(
                //    name: String,
                //    serializer: KSerializer<T>,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //    defaultValueProvider: DefaultValue<T>? = null
                //  )
                OPTIONAL_VALUE -> attributeIrCall(declaration, RhizomedbSymbolNames.optionalValueName) {
                    val serializer = generateSerializerCall(typeT, myContext, "???")
                    putValueArgument(1, serializer)
                    putValueArgument(2, flags ?: irNotIndexedEntry())
                    putValueArgument(3, irNull(defaultValueType))
                }
                // fun <T : Any> manyValues(
                //    name: String,
                //    serializer: KSerializer<T>,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //  )
                MANY_VALUES -> attributeIrCall(declaration, RhizomedbSymbolNames.manyValuesName) {
                    val serializer = generateSerializerCall(typeT, myContext, "???")
                    putValueArgument(1, serializer)
                    putValueArgument(2, flags ?: irNotIndexedEntry())
                }
                // fun <T : Any> requiredTransient(
                //    name: String,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //    defaultValueProvider: DefaultValue<T>? = null
                //  )
                REQUIRED_TRANSIENT -> attributeIrCall(declaration, RhizomedbSymbolNames.requiredTransientName) {
                    putValueArgument(1, flags ?: irNotIndexedEntry())
                    putValueArgument(2, irNull(defaultValueType))
                }
                // fun <T : Any> optionalTransient(
                //    name: String,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //    defaultValueProvider: DefaultValue<T>? = null
                //  )
                OPTIONAL_TRANSIENT -> attributeIrCall(declaration, RhizomedbSymbolNames.optionalTransientName) {
                    putValueArgument(1, flags ?: irNotIndexedEntry())
                    putValueArgument(2, irNull(defaultValueType))
                }
                // fun <T : Any> manyTransient(
                //    name: String,
                //    valueFlags: Indexing = Indexing.NOT_INDEXED,
                //  )
                MANY_TRANSIENT -> attributeIrCall(declaration, RhizomedbSymbolNames.manyTransientName) {
                    putValueArgument(1, flags.takeIf { it is IrVarargImpl && it.elements.size > 0 } ?: irNotIndexedEntry())
                }
                // fun <T : Entity> requiredRef(
                //    name: String,
                //    vararg refFlags: RefFlags
                //  )
                REQUIRED_REF -> attributeIrCall(declaration, RhizomedbSymbolNames.requiredRefName) {
                    putValueArgument(1, flags.takeIf { it is IrVarargImpl && it.elements.size > 0 } ?: irEmptyRefFlagsVararg())
                }
                // fun <T : Entity> optionalRef(
                //    name: String,
                //    vararg refFlags: RefFlags
                //  )
                OPTIONAL_REF -> attributeIrCall(declaration, RhizomedbSymbolNames.optionalRefName) {
                    putValueArgument(1, flags.takeIf { it is IrVarargImpl && it.elements.size > 0 } ?: irEmptyRefFlagsVararg())
                }
                // fun <T : Entity> manyRef(
                //    name: String,
                //    vararg refFlags: RefFlags
                //  )
                MANY_REF -> attributeIrCall(declaration, RhizomedbSymbolNames.manyRefName) {
                    putValueArgument(1, flags ?: irEmptyRefFlagsVararg())
                }
            }
        }

        val field = pluginContext.irFactory.buildField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("${declaration.name.identifier}-field")
            type = getter.returnType
            visibility = DescriptorVisibilities.PRIVATE_TO_THIS
            isStatic = pluginContext.platform.isJvm()
        }.also { f ->
            f.correspondingPropertySymbol = declaration.symbol
            f.parent = declaration.parent
            f.initializer = pluginContext.irFactory.createExpressionBody(-1, -1, call)
        }
        declaration.backingField = field
    }

    private fun IrBuilderWithScope.irEmptyRefFlagsVararg(): IrVarargImpl {
        val elementType = irBuiltIns.refFlags.defaultType
        return irVararg(elementType, emptyList())
    }

    private fun IrBuilderWithScope.irNotIndexedEntry(): IrGetEnumValueImpl =
        irGetEnumEntry(RhizomedbSymbolNames.indexingClassId, Name.identifier("NOT_INDEXED"))

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.irGetEnumEntry(enumClassId: ClassId, enumEntryName: Name): IrGetEnumValueImpl {
        val enum = pluginContext.referenceClass(enumClassId)!!.owner
        val entry = enum.declarations.filterIsInstance<IrEnumEntry>().first { it.name == enumEntryName }
        return IrGetEnumValueImpl(startOffset, endOffset, enum.defaultType, entry.symbol)
    }
}

enum class CallKind {
    REQUIRED_VALUE, OPTIONAL_VALUE, MANY_VALUES,
    REQUIRED_TRANSIENT, OPTIONAL_TRANSIENT, MANY_TRANSIENT,
    REQUIRED_REF, OPTIONAL_REF, MANY_REF,
}

fun AttributeAnnotationKind.join(kind: RhizomedbAttributeKind): CallKind {
    return when (this) {
        AttributeAnnotationKind.VALUE -> when (kind) {
            RhizomedbAttributeKind.REQUIRED -> REQUIRED_VALUE
            RhizomedbAttributeKind.OPTIONAL -> OPTIONAL_VALUE
            RhizomedbAttributeKind.MANY -> MANY_VALUES
        }
        AttributeAnnotationKind.TRANSIENT -> when (kind) {
            RhizomedbAttributeKind.REQUIRED -> REQUIRED_TRANSIENT
            RhizomedbAttributeKind.OPTIONAL -> OPTIONAL_TRANSIENT
            RhizomedbAttributeKind.MANY -> MANY_TRANSIENT
        }
        AttributeAnnotationKind.REF -> when (kind) {
            RhizomedbAttributeKind.REQUIRED -> REQUIRED_REF
            RhizomedbAttributeKind.OPTIONAL -> OPTIONAL_REF
            RhizomedbAttributeKind.MANY -> MANY_REF
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBuilderWithScope.attributeIrCall(declaration: IrProperty, name: Name, body: IrCall.() -> Unit): IrCall {
    val foo = declaration.parentClassOrNull!!.symbol.functionByName(name.asString())
    val type = (declaration.getter!!.returnType as IrSimpleType).arguments[0].typeOrFail
    return irCall(foo).apply {
        dispatchReceiver = irGetObject((declaration.parent as IrClass).symbol)
        putTypeArgument(0, type)
        putValueArgument(0, irString(declaration.name.asString()))
        body()
    }
}

//private fun

private data class AttributeAnnotation(val element: IrConstructorCall, val kind: AttributeAnnotationKind)

private fun IrProperty.attributeAnnotation(): AttributeAnnotation? {
    annotations.findAnnotation(RhizomedbAnnotations.valueAttributeFqName)?.let {
        return AttributeAnnotation(it, AttributeAnnotationKind.VALUE)
    }
    annotations.findAnnotation(RhizomedbAnnotations.transientAttributeFqName)?.let {
        return AttributeAnnotation(it, AttributeAnnotationKind.TRANSIENT)
    }
    annotations.findAnnotation(RhizomedbAnnotations.referenceAttributeFqName)?.let {
        return AttributeAnnotation(it, AttributeAnnotationKind.REF)
    }

    return null
}