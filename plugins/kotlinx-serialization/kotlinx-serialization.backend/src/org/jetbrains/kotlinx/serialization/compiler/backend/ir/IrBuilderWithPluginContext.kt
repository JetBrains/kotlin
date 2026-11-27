/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.irAnonymousInitializer
import org.jetbrains.kotlin.ir.builders.declarations.v2.irBackingField
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.builders.declarations.v2.irGetter
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.builders.expressions.v2.buildAnnotation
import org.jetbrains.kotlin.ir.builders.expressions.v2.buildClassReference
import org.jetbrains.kotlin.ir.builders.expressions.v2.irCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irExpressionBody
import org.jetbrains.kotlin.ir.builders.expressions.v2.irFunctionAccessExpression
import org.jetbrains.kotlin.ir.builders.expressions.v2.irFunctionExpression
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetEnumValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irReturn
import org.jetbrains.kotlin.ir.builders.expressions.v2.irSetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irVararg
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_NAME_FQ


interface IrBuilderWithPluginContext {
    val compilerContext: SerializationPluginContext

    fun <F: IrFunction> addFunctionBody(function: F, bodyGen: IrBlockBodyBuilder.(F) -> Unit) {
        val parentClass = function.parent
        val startOffset = function.startOffset.takeIf { it >= 0 } ?: parentClass.startOffset
        val endOffset = function.endOffset.takeIf { it >= 0 } ?: parentClass.endOffset
        function.body = DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(
            startOffset,
            endOffset
        ) { bodyGen(function) }
    }

    fun IrClass.createLambdaExpression(
        type: IrType,
        bodyGen: IrBlockBodyBuilder.() -> Unit
    ): IrFunctionExpression {
        val function = compilerContext.irFactory.irFunction {
            this.startOffset = this@createLambdaExpression.startOffset
            this.endOffset = this@createLambdaExpression.endOffset
            this.returnType = type
            name = Name.identifier("<anonymous>")
            visibility = DescriptorVisibilities.LOCAL
            origin = SERIALIZATION_PLUGIN_ORIGIN
            parent = this@createLambdaExpression
        }
        val irBuilder = DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset)
        function.body = irBuilder.irBlockBody(startOffset, endOffset) {
            val expr = addAndGetLastExpression(bodyGen)
            +irReturn { value = expr }
        }

        val f0Type = compilerContext.irBuiltIns.functionN(0)
        val f0ParamSymbol = f0Type.typeParameters[0].symbol
        val f0IrType = f0Type.defaultType.substitute(mapOf(f0ParamSymbol to type))

        return irBuilder.irFunctionExpression {
            this.type = f0IrType
            this.function = function
            origin = IrStatementOrigin.LAMBDA
        }
    }

    fun addLazyValProperty(
        containingClass: IrClass,
        targetIrType: IrType,
        propertyName: Name,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        val lazyIrType = lazyType(targetIrType)

        val prop = containingClass.irProperty {
            startOffset = containingClass.startOffset
            endOffset = containingClass.endOffset
            name = propertyName
            this.visibility = visibility
            this.isVar = false
            origin = SERIALIZATION_PLUGIN_ORIGIN
            backingField {
                startOffset = containingClass.startOffset
                endOffset = containingClass.endOffset
                name = Name.identifier(propertyName.asString() + "\$delegate")
                type = lazyIrType
                origin = SERIALIZATION_PLUGIN_ORIGIN
                isFinal = true
            }
            getter {
                startOffset = containingClass.startOffset
                endOffset = containingClass.endOffset
                returnType = targetIrType
                origin = SERIALIZATION_PLUGIN_ORIGIN
                this.visibility = visibility
                modality = Modality.FINAL
            }
        }
        val field = prop.backingField!!

        containingClass.addAnonymousInit {
            val invokeLazyExpr = createLazyDelegate(targetIrType, containingClass, initializerBuilder)
            +irSetField(field.symbol) {
                receiver = irGetValue(containingClass.thisReceiver!!.symbol)
                value = invokeLazyExpr
            }
        }

        prop.getter!!.apply {
            body = compilerContext.irBuiltIns.createIrBuilder(symbol, containingClass.startOffset, containingClass.endOffset).irBlockBody {
                +irReturn {
                    value = irInvoke(
                        compilerContext.lazyValueGetter,
                        irGetField(field.symbol) {
                            receiver = irGetValue(dispatchReceiverParameter!!.symbol)
                        },
                        returnTypeHint = targetIrType
                    )
                }
            }
        }
        return prop
    }

    fun IrElement.createAnnotationCallWithoutArgs(annotationSymbol: IrClassSymbol): IrAnnotation {
        val annotationCtor = annotationSymbol.constructors.single { it.owner.isPrimary }
        val annotationType = annotationSymbol.defaultType

        val element = this
        return buildAnnotation(annotationCtor) {
            startOffset = element.startOffset
            endOffset = element.endOffset
            type = annotationType
        }
    }

    fun IrClass.addValPropertyWithJvmField(
        type: IrType,
        name: Name,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        return generateSimplePropertyWithBackingField(name, type, this, visibility).apply {
            val field = backingField!!
            addAnonymousInit {
                val resultExpression = addAndGetLastExpression(initializerBuilder)
                +irSetField(field.symbol) {
                    receiver = irGetValue(thisReceiver!!.symbol)
                    value = resultExpression
                }
            }
            field.annotations += createAnnotationCallWithoutArgs(compilerContext.jvmFieldClassSymbol)
        }
    }

    fun IrClass.addValPropertyWithJvmFieldInitializer(
        type: IrType,
        name: Name,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        initializer: IrBuilderWithScope.() -> IrExpression
    ): IrProperty {
        return generateSimplePropertyWithBackingField(name, type, this, visibility).apply {
            val field = backingField!!

            val builder = DeclarationIrBuilder(
                compilerContext,
                field.symbol,
                field.startOffset,
                field.endOffset
            )
            val initializerExpression = builder.initializer()
            field.initializer = factory.irExpressionBody {
                startOffset = initializerExpression.startOffset
                endOffset = initializerExpression.endOffset
                expression = initializerExpression
            }
            field.annotations += createAnnotationCallWithoutArgs(compilerContext.jvmFieldClassSymbol)
        }
    }

    /**
     * Add all statements to the builder, except the last one.
     * The last statement should be an expression, it will return as a result
     */
    private fun IrStatementsBuilder<*>.addAndGetLastExpression(blockBuilder: IrBlockBodyBuilder.() -> Unit): IrExpression {
        val irBlockBody = irBlockBody(startOffset, endOffset, blockBuilder)
        irBlockBody.statements.dropLast(1).forEach { +it }
        return irBlockBody.statements.last() as? IrExpression
            ?: error("Last statement in property initializer builder is not an a expression")
    }

    context(irBuilder: IrBuilderWithScope) fun irInvoke(
        callee: IrFunctionSymbol,
        vararg arguments: IrExpression,
        returnTypeHint: IrType? = null,
    ): IrFunctionAccessExpression {
        return irBuilder.irFunctionAccessExpression(callee) {
            type = returnTypeHint ?: callee.owner.returnType
            this.arguments.assignFrom(arguments.toList())
        }
    }

    context(irBuilder: IrBuilderWithScope) fun irInvoke(
        callee: IrFunctionSymbol,
        arguments: List<IrExpression>,
        typeArguments: List<IrType?> = emptyList(),
        returnTypeHint: IrType? = null,
    ): IrFunctionAccessExpression {
        return irBuilder.irFunctionAccessExpression(callee) {
            type = returnTypeHint ?: callee.owner.returnType
            this.arguments.assignFrom(arguments.toList())
            this.typeArguments.assignFrom(typeArguments)
        }
    }

    context(irBuilder: IrBuilderWithScope) fun createArrayOfExpression(
        arrayElementType: IrType,
        arrayElements: List<IrExpression>,
    ): IrExpression {

        val arrayType = compilerContext.irBuiltIns.arrayClass.typeWith(arrayElementType)
        val arg0 = irBuilder.irVararg {
            type = arrayType
            varargElementType = arrayElementType
            elements += arrayElements
        }

        return irBuilder.irCall(compilerContext.irBuiltIns.arrayOf) {
            type = arrayType
            this.typeArguments[0] = arrayElementType
            arguments[0] = arg0
        }
    }

    fun IrBuilderWithScope.createIntArrayOfExpression(arrayElements: List<IrExpression>): IrExpression {
        val elementType = compilerContext.irBuiltIns.intType
        val arrayType = compilerContext.intArrayOfFunctionSymbol.owner.returnType
        val arg0 = irVararg {
            type = arrayType
            varargElementType = elementType
            elements += arrayElements
        }
        return irCall(compilerContext.intArrayOfFunctionSymbol) {
            type = arrayType
            arguments[0] = arg0
        }
    }

    fun IrClass.addAnonymousInit(body: IrBlockBodyBuilder.() -> Unit) {
        val irClass = this
        val anonymousInit = irAnonymousInitializer {
            startOffset = irClass.startOffset
            endOffset = irClass.endOffset
            origin = SERIALIZATION_PLUGIN_ORIGIN
        }

        anonymousInit.body =
            DeclarationIrBuilder(
                compilerContext,
                anonymousInit.symbol,
                anonymousInit.startOffset,
                anonymousInit.endOffset
            ).irBlockBody(body = body)
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val classFqName = (lhs.type as IrSimpleType).classOrNull!!.owner.fqNameWhenAvailable!!
        val symbol = compilerContext
            .finderForSource((scope.scopeOwnerSymbol.owner as IrDeclaration).file)
            .findFunctions(CallableId(ClassId.topLevel(classFqName), name))
            .single()
        return irInvoke(symbol, lhs, rhs)
    }

    fun IrBuilderWithScope.setProperty(receiver: IrExpression, property: IrProperty, value: IrExpression): IrExpression {
        val setter = property.setter
        return if (setter != null)
            irCall(setter.symbol) {
                type = setter.returnType
                origin = IrStatementOrigin.EQ
                arguments[0] = receiver
                arguments[1] = value
            }
        else
            irSetField(property.backingField!!.symbol) {
                this.receiver = receiver
                this.value = value
            }
    }

    fun IrBuilderWithScope.generateAnySuperConstructorCall(toBuilder: IrBlockBodyBuilder) {
        val anyConstructor = compilerContext.irBuiltIns.anyClass.owner.declarations.single { it is IrConstructor } as IrConstructor
        with(toBuilder) {
            +irDelegatingConstructorCall(anyConstructor.symbol)
        }
    }

    fun generateSimplePropertyWithBackingField(
        propertyName: Name,
        propertyType: IrType,
        propertyParent: IrClass,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE
    ): IrProperty = generatePropertyMissingParts(null, propertyName, propertyType, propertyParent, visibility)

    fun generatePropertyMissingParts(
        property: IrProperty?,
        propertyName: Name,
        propertyType: IrType,
        propertyParent: IrClass,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE
    ): IrProperty {
        val prop = property ?: propertyParent.irProperty {
            startOffset = propertyParent.startOffset
            endOffset = propertyParent.endOffset
            name = propertyName
            this.isVar = false
            origin = SERIALIZATION_PLUGIN_ORIGIN
        }

        val field = prop.backingField ?: prop.irBackingField {
            startOffset = propertyParent.startOffset
            endOffset = propertyParent.endOffset
            name = propertyName
            type = propertyType
            origin = SERIALIZATION_PLUGIN_ORIGIN
            isFinal = true
        }

        val getter = prop.getter ?: prop.irGetter {
            startOffset = propertyParent.startOffset
            endOffset = propertyParent.endOffset
            returnType = propertyType
            origin = SERIALIZATION_PLUGIN_ORIGIN
            this.visibility = visibility
            modality = Modality.FINAL
        }

        getter.apply {
            if (parameters.isEmpty())
                parameters = listOf(propertyParent.thisReceiver!!.copyTo(this, type = propertyParent.defaultType))
            if (body == null)
                body = compilerContext.irBuiltIns.createIrBuilder(symbol, propertyParent.startOffset, propertyParent.endOffset).irBlockBody {
                        +irReturn {
                            value = irGetField(field.symbol) {
                                receiver = irGetValue(dispatchReceiverParameter!!.symbol)
                            }
                        }
                    }
        }
        return prop
    }

    fun createClassReference(classType: IrType, startOffset: Int, endOffset: Int): IrClassReference {
        return buildClassReference(classType.classifierOrFail) {
            this.startOffset = startOffset
            this.endOffset = endOffset
            type = compilerContext.irBuiltIns.kClassClass.starProjectedType
            this.classType = classType
        }
    }

    context(irBuilder: IrBuilderWithScope) fun classReference(classSymbol: IrClassSymbol): IrClassReference =
        createClassReference(classSymbol.starProjectedType, irBuilder.startOffset, irBuilder.endOffset)

    fun collectSerialInfoAnnotations(irClass: IrClass): List<IrAnnotation> {
        if (!(irClass.isInterface || irClass.hasSerializableOrMetaAnnotation())) return emptyList()
        val annotationByFq: MutableMap<FqName, List<IrAnnotation>> =
            irClass.annotations.groupBy { it.classSymbol.owner.fqNameWhenAvailable!! }.toMutableMap()
        for (clazz in irClass.getAllSuperclasses()) {
            val annotations = clazz.annotations
                .mapNotNull {
                    val parent = it.classSymbol.owner
                    if (parent.isInheritableSerialInfoAnnotation) parent.fqNameWhenAvailable!! to it else null
                }
            annotations.forEach { [fqname, call] ->
                if (fqname !in annotationByFq) {
                    annotationByFq[fqname] = listOf(call)
                } else {
                    // SerializationPluginDeclarationChecker already reported inconsistency
                    // InheritableSerialInfo annotations can not be repeatable
                }
            }
        }
        return annotationByFq.values.toList().flatten()
    }

    fun copyAnnotationsFrom(annotations: List<IrAnnotation>): List<IrExpression> =
        annotations.filter { it.classSymbol.owner.isSerialInfoAnnotation }.map { it.deepCopyWithoutPatchingParents() }

    fun kSerializerType(serializableType: IrType): IrSimpleType {
        val kSerializerClass = compilerContext.kSerializerClass
            ?: error("Serializer class '$KSERIALIZER_NAME_FQ' not found. Check that the kotlinx.serialization runtime is connected correctly")
        return kSerializerClass.typeWith(serializableType)
    }

    fun lazyType(returnType: IrType): IrType {
        return compilerContext.lazyClass.defaultType.substitute(mapOf(compilerContext.lazyClass.typeParameters[0].symbol to returnType))
    }

    fun IrBuilderWithScope.createLazyDelegate(
        returnType: IrType,
        containingClass: IrClass,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit,
    ): IrExpression {
        val lazyIrType = lazyType(returnType)

        val enumElement = irGetEnumValue(compilerContext.lazyModePublicationEnumEntry.symbol) {
            type = compilerContext.lazyModeClass.defaultType
        }
        val lambdaExpression = containingClass.createLambdaExpression(returnType, initializerBuilder)
        return irInvoke(compilerContext.lazyFunctionSymbol, listOf(enumElement, lambdaExpression), listOf(returnType), lazyIrType)
    }

    context(irBuilder: IrBuilderWithScope)
    fun wrapperClassReference(classType: IrType): IrClassReference {
        if (compilerContext.platform.isJvm()) {
            // "Byte::class" -> "java.lang.Byte::class"
            val wrapperFqName = classType.getPrimitiveType()?.let(JvmPrimitiveType::get)?.wrapperFqName
            if (wrapperFqName != null) {
                val wrapperClass = compilerContext.finderForBuiltins().findClass(ClassId.topLevel(wrapperFqName))
                    ?: error("Primitive wrapper class for $classType not found: $wrapperFqName")
                return createClassReference(wrapperClass.defaultType, irBuilder.startOffset, irBuilder.endOffset)
            }
        }
        return createClassReference(classType, irBuilder.startOffset, irBuilder.endOffset)
    }

    fun IrClass.getSuperClassOrAny(): IrClass = getSuperClassNotAny() ?: compilerContext.irBuiltIns.anyClass.owner
}
