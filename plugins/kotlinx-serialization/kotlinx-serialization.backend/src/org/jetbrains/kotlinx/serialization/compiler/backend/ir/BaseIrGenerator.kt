/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.objectSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.sealedSerializerId

abstract class BaseIrGenerator(private val currentClass: IrClass, final override val compilerContext: SerializationPluginContext) :
    IrBuilderWithPluginContext {

    private val throwMissedFieldExceptionFunc = compilerContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_NAME
        )
    ).singleOrNull()

    private val throwMissedFieldExceptionArrayFunc = compilerContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_NAME
        )
    ).singleOrNull()

    private val enumSerializerFactoryFunc = compilerContext.enumSerializerFactoryFunc

    private val annotatedEnumSerializerFactoryFunc = compilerContext.annotatedEnumSerializerFactoryFunc

    fun useFieldMissingOptimization(): Boolean {
        return throwMissedFieldExceptionFunc != null && throwMissedFieldExceptionArrayFunc != null
    }

    fun IrDeclaration.excludeFromJsExport() {
        if (!compilerContext.platform.isJs()) {
            return
        }
        val jsExportIgnore = compilerContext.jsExportIgnoreClass ?: return
        val jsExportIgnoreCtor = jsExportIgnore.primaryConstructor ?: return

        annotations += IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            jsExportIgnore.defaultType,
            jsExportIgnoreCtor.symbol,
            jsExportIgnore.typeParameters.size,
            jsExportIgnoreCtor.typeParameters.size,
        )
    }


    private fun getClassListFromFileAnnotation(annotationFqName: FqName): List<IrClassSymbol> {
        val annotation = currentClass.fileParent.annotations.findAnnotation(annotationFqName) ?: return emptyList()
        val vararg = annotation.arguments[0] as? IrVararg ?: return emptyList()
        return vararg.elements
            .mapNotNull { (it as? IrClassReference)?.symbol as? IrClassSymbol }
    }

    val contextualKClassListInCurrentFile: Set<IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(
            SerializationAnnotations.contextualFqName,
        ).plus(
            getClassListFromFileAnnotation(
                SerializationAnnotations.contextualOnFileFqName,
            )
        ).toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<Pair<IrClassSymbol, Boolean>, IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName)
            .associateBy(
                { serializerSymbol ->
                    val kotlinType = getAllSubstitutedSupertypes(serializerSymbol.owner).find(IrType::isKSerializer)?.arguments?.firstOrNull()?.typeOrNull
                    val classSymbol = kotlinType?.classOrNull
                        ?: error("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                    classSymbol to kotlinType.isNullable()
                },
                { it }
            )
    }

    fun IrBlockBodyBuilder.generateGoldenMaskCheck(
        seenVars: List<IrValueDeclaration>,
        properties: IrSerializableProperties,
        serialDescriptor: IrExpression
    ) {
        val fieldsMissedTest: IrExpression
        val throwErrorExpr: IrExpression

        val maskSlotCount = seenVars.size
        if (maskSlotCount == 1) {
            val goldenMask = properties.goldenMask


            throwErrorExpr = irInvoke(
                throwMissedFieldExceptionFunc!!,
                irGet(seenVars[0]),
                irInt(goldenMask),
                serialDescriptor,
                returnTypeHint = compilerContext.irBuiltIns.unitType
            )

            fieldsMissedTest = irNotEquals(
                irInt(goldenMask),
                irBinOp(
                    OperatorNameConventions.AND,
                    irInt(goldenMask),
                    irGet(seenVars[0])
                )
            )
        } else {
            val goldenMaskList = properties.goldenMaskList

            var compositeExpression: IrExpression? = null
            for (i in goldenMaskList.indices) {
                val singleCheckExpr = irNotEquals(
                    irInt(goldenMaskList[i]),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irInt(goldenMaskList[i]),
                        irGet(seenVars[i])
                    )
                )

                compositeExpression = if (compositeExpression == null) {
                    singleCheckExpr
                } else {
                    irBinOp(
                        OperatorNameConventions.OR,
                        compositeExpression,
                        singleCheckExpr
                    )
                }
            }

            fieldsMissedTest = compositeExpression!!

            throwErrorExpr = irBlock {
                +irInvoke(
                    throwMissedFieldExceptionArrayFunc!!,
                    createIntArrayOfExpression(goldenMaskList.indices.map { irGet(seenVars[it]) }),
                    createIntArrayOfExpression(goldenMaskList.map { irInt(it) }),
                    serialDescriptor,
                    returnTypeHint = compilerContext.irBuiltIns.unitType
                )
            }
        }

        +irIfThen(compilerContext.irBuiltIns.unitType, fieldsMissedTest, throwErrorExpr)
    }

    fun IrBlockBodyBuilder.serializeAllProperties(
        serializableProperties: List<IrSerializableProperty>,
        objectToSerialize: IrValueDeclaration,
        localOutput: IrValueDeclaration,
        localSerialDesc: IrValueDeclaration,
        kOutputClass: IrClassSymbol,
        ignoreIndexTo: Int,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
        cachedChildSerializerByIndex: (Int) -> IrExpression?,
        genericGetter: ((Int, IrType) -> IrExpression)?
    ) {

        fun IrSerializableProperty.irGet(): IrExpression {
            val ownerType = objectToSerialize.symbol.owner.type
            val propertyType = this.type
            return getProperty(irGet(ownerType, objectToSerialize.symbol), ir, propertyType)
        }

        for ((index, property) in serializableProperties.withIndex()) {
            if (index < ignoreIndexTo) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val elementCall = formEncodeDecodePropertyCall(
                irGet(localOutput),
                property, { innerSerial, sti ->
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                    f to listOf(
                        irGet(localSerialDesc),
                        irInt(index),
                        innerSerial,
                        property.irGet()
                    )
                }, {
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}")
                    val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                    if (it.elementMethodPrefix != "Unit") args.add(property.irGet())
                    f to args
                },
                cachedChildSerializerByIndex(index),
                genericGetter
            )

            // check for call to .shouldEncodeElementDefault
            val encodeDefaults = property.ir.getEncodeDefaultAnnotationValue()
            val field =
                property.ir.backingField
            val initializer = field?.initializer // FIXME: Null when property from another module; can't compare it with default value on JS or Native
            if (!property.optional || encodeDefaults == true || field == null || initializer == null) {
                // emit call right away
                +elementCall
            } else {
                val partB = irNotEquals(property.irGet(), initializerAdapter(initializer))

                val condition = if (encodeDefaults == false) {
                    // drop default without call to .shouldEncodeElementDefault
                    partB
                } else {
                    // emit check:
                    // if (if (output.shouldEncodeElementDefault(this.descriptor, i)) true else {obj.prop != DEFAULT_VALUE} ) {
                    //    output.encodeIntElement(this.descriptor, i, obj.prop)// block {obj.prop != DEFAULT_VALUE} may contain several statements
                    val shouldEncodeFunc = kOutputClass.functionByName(CallingConventions.shouldEncodeDefault)
                    val partA = irInvoke(shouldEncodeFunc, irGet(localOutput), irGet(localSerialDesc), irInt(index))
                    // Ir infrastructure does not have dedicated symbol for ||, so
                    //  `a || b == if (a) true else b`, see org.jetbrains.kotlin.ir.builders.PrimitivesKt.oror
                    irIfThenElse(compilerContext.irBuiltIns.booleanType, partA, irTrue(), partB)
                }
                +irIfThen(condition, elementCall)
            }
        }
    }


    fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        encoder: IrExpression,
        property: IrSerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: IrSerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: IrSerialTypeInfo) -> FunctionWithArgs,
        cachedSerializer: IrExpression?,
        genericGetter: ((Int, IrType) -> IrExpression)? = null,
        returnTypeHint: IrType? = null
    ): IrExpression {
        val sti = getIrSerialTypeInfo(property, compilerContext)
        val innerSerial = cachedSerializer ?: serializerInstance(
            sti.serializer,
            compilerContext,
            property.type,
            property.genericIndex,
            property.ir.parentClassOrNull,
            genericGetter
        )
        val (functionToCall, args: List<IrExpression>) = if (innerSerial != null) whenHaveSerializer(innerSerial, sti) else whenDoNot(sti)
        val typeArgs = if (functionToCall.owner.typeParameters.isNotEmpty()) listOf(property.type) else listOf()
        return irInvoke(functionToCall, listOf(encoder) + args, typeArgs, returnTypeHint)
    }

    context(irBuilder: IrBuilderWithScope) internal fun callSerializerFromCompanion(
        thisIrType: IrSimpleType,
        typeArgs: List<IrType>,
        args: List<IrExpression>,
        expectedSerializer: ClassId?
    ): IrExpression? {
        val baseClass = thisIrType.getClass() ?: return null
        val companionClass = baseClass.companionObject() ?: return null
        val serializerProviderFunction = companionClass.declarations.singleOrNull {
            it is IrFunction && it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && it.nonDispatchParameters.size == baseClass.typeParameters.size
        } ?: return null

        // workaround for sealed and abstract classes - the `Companion.serializer()` function expects non-null serializers, but does not use them, so serializers of any type can be passed
        val replaceArgsWithUnitSerializer = expectedSerializer == polymorphicSerializerId

        val adjustedArgs: List<IrExpression> =
            if (replaceArgsWithUnitSerializer) {
                val serializer = compilerContext.unitSerializerClass!!
                List(baseClass.typeParameters.size) { irBuilder.irGetObject(serializer) }
            } else {
                args
            }

        val adjustedTypeArgs: List<IrType> = if (replaceArgsWithUnitSerializer) thisIrType.argumentTypesOrUpperBounds() else typeArgs

        with(serializerProviderFunction as IrFunction) {
            // Note that [typeArgs] may be unused if we short-cut to e.g. SealedClassSerializer
            return irInvoke(
                symbol,
                listOf(irBuilder.irGetObject(companionClass)) + adjustedArgs.takeIf { it.size == nonDispatchParameters.size }.orEmpty(),
                adjustedTypeArgs.takeIf { it.size == typeParameters.size }.orEmpty(),
            )
        }
    }

    context(irBuilder: IrBuilderWithScope) internal fun callSerializerFromObject(
        thisIrType: IrSimpleType,
        args: List<IrExpression>,
        // type parameters not allowed for object class, so its missed
    ): IrExpression? {
        val baseClass = thisIrType.getClass() ?: return null
        val serializerProviderFunction = baseClass.declarations.singleOrNull {
            it is IrFunction && it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && it.nonDispatchParameters.size == baseClass.typeParameters.size
        } ?: return null

        with(serializerProviderFunction as IrFunction) {
            return irInvoke(
                symbol,
                listOf(irBuilder.irGetObject(baseClass)) + args.takeIf { it.size == nonDispatchParameters.size }.orEmpty(),
            )
        }
    }

    // Does not use sti and therefore does not perform encoder calls optimization
    fun IrBuilderWithScope.serializerTower(
        generator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        property: IrSerializableProperty,
        cachedSerializer: IrExpression?
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()

        val serializerExpression = if (cachedSerializer != null) {
            cachedSerializer
        } else {
            val serializerClassSymbol =
                property.serializableWith(compilerContext)
                    ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                        compilerContext,
                        property.type
                    ) else null

            serializerInstance(
                serializerClassSymbol,
                compilerContext,
                property.type,
                genericIndex = property.genericIndex,
                property.ir.parentClassOrNull,
            ) { it, _ ->
                val ir = generator.localSerializersFieldsDescriptors[it]
                irGetField(irGet(dispatchReceiverParameter), ir.backingField!!)
            }
        }

        return serializerExpression?.let { expr -> wrapWithNullableSerializerIfNeeded(property.type, expr, nullableSerClass) }
    }

    context(irBuilder: IrBuilderWithScope) internal fun wrapWithNullableSerializerIfNeeded(
        type: IrType,
        expression: IrExpression,
        nullableProp: IrPropertySymbol
    ): IrExpression = if (type.isMarkedNullable()) {
        val resultType = type.makeNotNull()
        val typeArguments = listOf(resultType)
        val callee = nullableProp.owner.getter!!

        val returnType = callee.returnType.substitute(callee.typeParameters, typeArguments)

        irInvoke(
            callee = callee.symbol,
            arguments = listOf(expression),
            typeArguments = typeArguments,
            returnTypeHint = returnType
        )
    } else {
        expression
    }

    fun wrapIrTypeIntoKSerializerIrType(
        type: IrType,
        variance: Variance = Variance.INVARIANT
    ): IrType {
        val kSerClass = compilerContext.referenceClass(ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME))
            ?: error("Couldn't find class ${SerialEntityNames.KSERIALIZER_NAME}")
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, variance)
            ), annotations = emptyList()
        )
    }

    internal fun IrClass.addCachedChildSerializersProperty(cacheableSerializers: List<IrExpression?>): IrProperty? {
        // if all child serializers are null (non-cacheable) we don't need to create a property
        cacheableSerializers.firstOrNull { it != null } ?: return null

        val kSerializerType = kSerializerType(compilerContext.irBuiltIns.anyType)
        val elementType = lazyType(kSerializerType)
        val arrayType = compilerContext.irBuiltIns.arrayClass.typeWith(elementType)

        val property = addValPropertyWithJvmFieldInitializer(arrayType, SerialEntityNames.CACHED_CHILD_SERIALIZERS_PROPERTY_NAME) {
            createArrayOfExpression(elementType, cacheableSerializers.map { it ?: irNull() })
        }

        if (declarations.removeIf { declaration -> declaration === property }) {
            // adding the property very first because children can be used even in first constructor
            declarations.add(0, property)
        }


        return property
    }

    /**
     * Factory to getting cached serializers via variable.
     * Must be used only in one place because for each factory creates one variable.
     *
     * Class from [containingClassProducer] used only if [cacheProperty] is not null.
     */
    internal fun IrStatementsBuilder<*>.createCacheableChildSerializersFactory(
        cacheProperty: IrProperty?,
        cacheableSerializers: List<Boolean>,
        containingClassProducer: () -> IrClass
    ): (Int) -> IrExpression? {
        cacheProperty ?: return { null }

        val kSerializerType = this@BaseIrGenerator.kSerializerType(compilerContext.irBuiltIns.anyType)

        val variable =
            irTemporary(irInvoke(cacheProperty.getter!!.symbol, irGetObject(containingClassProducer())), "cached")

        return { index: Int ->
            if (cacheableSerializers[index]) {
                val lazyDelegate = irInvoke(compilerContext.arrayValueGetter.symbol, irGet(variable), irInt(index))
                irInvoke(
                    compilerContext.lazyValueGetter,
                    lazyDelegate,
                    returnTypeHint = kSerializerType
                )
            } else {
                null
            }
        }
    }

    fun IrClass.createCachedChildSerializers(
        serializableClass: IrClass,
        serializableProperties: List<IrSerializableProperty>
    ): List<IrExpression?> {
        return DeclarationIrBuilder(compilerContext, symbol).run {
            serializableProperties.map { cacheableChildSerializerInstance(serializableClass, it) }
        }
    }

    private fun IrBuilderWithScope.cacheableChildSerializerInstance(
        serializableClass: IrClass,
        property: IrSerializableProperty
    ): IrExpression? {
        val serializer = getIrSerialTypeInfo(property, compilerContext).serializer ?: return null
        if (serializer.owner.kind == ClassKind.OBJECT) return null

        val kSerializerType = kSerializerType(property.type)

        // if in type arguments there are type parameters - we can't cache it in companion's property because she should use actual serializer
        val serializers = createSerializerOnlyForClasses(property.type, serializableClass) ?: return null
        val genericGetter: (Int, IrType) -> IrExpression = { index, _ ->
            serializers[index]
        }

        val expr = serializerInstance(
            serializer,
            compilerContext,
            property.type,
            null,
            serializableClass,
            genericGetter
        )

        return if (expr != null) {
            createLazyDelegate(kSerializerType, serializableClass) {
                +requireNotNull(expr)
            }
        } else {
            null
        }
    }

    // create serializers for type arguments if all arguments are classes, `null` otherwise
    private fun IrBuilderWithScope.createSerializerOnlyForClasses(type: IrSimpleType, serializableClass: IrClass): List<IrExpression>? {
        // arguments contain star projections or type parameter
        if (!type.arguments.all { it is IrSimpleType && it.typeOrNull?.isTypeParameter() != true }) {
            return null
        }

        return type.arguments.map { argumentType ->
            argumentType as? IrSimpleType ?: return null
            val serializer = findTypeSerializerOrContext(compilerContext, argumentType)
            serializerInstance(
                serializer,
                compilerContext,
                argumentType,
                null,
                serializableClass,
                null
            ) ?: return null // we can't create serializer
        }
    }

    private fun IrSimpleType.checkTypeArgumentsHasSelf(itselfClass: IrClassSymbol): Boolean {
        arguments.forEach { typeArgument ->
            if (typeArgument.typeOrNull?.classifierOrNull == itselfClass) return true
            if (typeArgument is IrSimpleType) {
                if (typeArgument.checkTypeArgumentsHasSelf(itselfClass)) return true
            }
        }

        return false
    }

    fun IrBuilderWithScope.serializerInstance(
        serializerClassOriginal: IrClassSymbol?,
        pluginContext: SerializationPluginContext,
        kType: IrType,
        genericIndex: Int? = null,
        rootSerializableClass: IrClass? = null,
        genericGetter: ((Int, IrType) -> IrExpression)? = null,
    ): IrExpression? {
        val inst = Instantiator(this@BaseIrGenerator, pluginContext, rootSerializableClass, genericGetter)
        return inst.serializerInstance(serializerClassOriginal, kType, genericIndex)
    }
}
