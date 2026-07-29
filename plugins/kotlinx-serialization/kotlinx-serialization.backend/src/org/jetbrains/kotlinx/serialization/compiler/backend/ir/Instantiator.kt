/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.objectSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.sealedSerializerId

internal class Instantiator(
    val generator: BaseIrGenerator,
    val compilerContext: SerializationPluginContext,
    // class for which the serializer is generated
    val rootSerializableClass: IrClass? = null,
    /**
     * Function to get expression for getting serializer by the index in the current generic context.
     *
     * Serialization context depends on where a serializer will be instantiated:
     * - Implementation of the serializer() function. Values - parameters of the serializer function. The indexes correspond to the indexes of the type parameters in the serializable class
     * - Implementation of the write$Self function. Values - parameters of the write$Self function. The indexes correspond to the indexes of the type parameters in the serializable class
     * - Implementation of serialize(), deserialize(), childSerializers() functions. Values - fields of the `$serializer` class. The indexes correspond to the indexes of the type parameters in the serializable class
     * - Serializer cache, called in the initializer of a property in the companion object. Values are serializers for direct type arguments of the property type.
     */
    val genericGetter: ((Int, IrType) -> IrExpression)? = null,
) {
    val nullableSerClass = compilerContext.finderForBuiltins().findProperties(SerialEntityNames.wrapIntoNullableCallableId).single()

    val hasEnumFactories =
        compilerContext.enumSerializerFactoryFunc != null && compilerContext.annotatedEnumSerializerFactoryFunc != null

    data class Args(
        val args: List<IrExpression>, val typeArgs: List<IrType>,
    )

    context(irBuilder: IrBuilderWithScope)
    fun serializerInstance(
        serializerClass: IrClassSymbol?,
        kType: IrType,
        genericIndex: Int? = null,
    ): IrExpression? {
        if (serializerClass == null) {
            if (genericIndex == null) return null
            return genericGetter?.invoke(genericIndex, kType)
        }
        if (serializerClass.owner.kind == ClassKind.OBJECT) {
            return getSingletonSerializer(serializerClass, kType)
        }

        kType as? IrSimpleType ?: error("Don't know how to work with type ${kType::class}")
        val typeArgumentsAsTypes = kType.argumentTypesOrUpperBounds()

        val needToCopyAnnotations = serializerClass.owner.classId.let { it == polymorphicSerializerId || it == objectSerializerId }

        // If KType is interface, .classSerializer always yields PolymorphicSerializer, which may be unavailable for interfaces from other modules
        val canUseShortcut =
            !kType.isInterface() && serializerClass == kType.classOrUpperBound()?.owner.classSerializer(compilerContext) && generator !is SerializableCompanionIrGenerator

        var ctor: IrConstructor? = null

        val (args, typeArgs) = when (serializerClass.owner.classId) {
            polymorphicSerializerId -> Args(listOf(classReferenceOf(kType)), listOf(kType))
            contextSerializerId -> argsForContextSerializer(kType, genericIndex, typeArgumentsAsTypes) ?: return null
            objectSerializerId -> Args(
                args = listOf(irBuilder.irString(kType.serialName()), irBuilder.irGetObject(kType.classOrUpperBound()!!)),
                typeArgs = listOf(kType),
            )
            sealedSerializerId -> return instantiateSealedSerializer(serializerClass, kType)
            enumSerializerId -> return instantiateEnumSerializer(serializerClass, kType)
            referenceArraySerializerId -> {
                (val origArgs = args, val origTypeArgs = typeArgs) = regularArgs(typeArgumentsAsTypes) ?: return null
                val args = listOf(generator.wrapperClassReference(typeArgumentsAsTypes.single())) + origArgs
                val typeArgs = listOf(origTypeArgs[0].makeNotNull()) + origTypeArgs
                Args(args, typeArgs)
            }
            else -> {
                if (canUseShortcut) {
                    regularArgs(typeArgumentsAsTypes) ?: return null
                } else {
                    ctor = findConstructor(serializerClass, needToCopyAnnotations).owner
                    val requiresArgs = ctor.parameters.isNotEmpty()
                    if (!requiresArgs) Args(emptyList(), emptyList()) else regularArgs(typeArgumentsAsTypes) ?: return null
                }
            }
        }

        if (canUseShortcut) {
            // This is default type serializer, we can shortcut through Companion.serializer()
            // BUT not during generation of this method itself
            // For future: check if we still want to build args for polymorphic/object serializers here, likely not.
            generator.callSerializerFromCompanion(kType, typeArgs, args, serializerClass.owner.classId)?.let { return it }
        }

        val newArgs = if (needToCopyAnnotations) addAnnotationsToArgs(kType, args) else args

        if (ctor == null) ctor = findConstructor(serializerClass, needToCopyAnnotations).owner
        return callConstructor(ctor, typeArgs, newArgs)
    }

    context(irBuilder: IrBuilderWithScope)
    private fun callConstructor(
        ctor: IrConstructor,
        typeArgs: List<IrType>,
        valueArgs: List<IrExpression>,
    ): IrFunctionAccessExpression {
        val typeParameters = ctor.parentAsClass.typeParameters
        val substitutedReturnType = ctor.returnType.substitute(typeParameters, typeArgs)
        return generator.irInvoke(
            ctor.symbol,
            // User may declare serializer with fixed type arguments, e.g. class SomeSerializer : KSerializer<ClosedRange<Float>>
            arguments = valueArgs.takeIf { it.size == ctor.parameters.size }.orEmpty(),
            typeArguments = typeArgs.takeIf { it.size == ctor.typeParameters.size }.orEmpty(),
            returnTypeHint = substitutedReturnType
        )
    }

    context(irBuilder: IrBuilderWithScope)
    private fun addAnnotationsToArgs(
        kType: IrSimpleType,
        args: List<IrExpression>,
    ): List<IrExpression> {
        val classAnnotations =
            generator.copyAnnotationsFrom(kType.getClass()?.let { generator.collectSerialInfoAnnotations(it) }.orEmpty())
        return args + generator.createArrayOfExpression(compilerContext.irBuiltIns.annotationType, classAnnotations)
    }

    private fun findConstructor(serializerClass: IrClassSymbol, needToCopyAnnotations: Boolean): IrConstructorSymbol {
        val serializable = getSerializableClassDescriptorBySerializer(serializerClass.owner)
        val ctor = if (serializable?.typeParameters?.isNotEmpty() == true) {
            requireNotNull(
                findSerializerConstructorForTypeArgumentsSerializers(serializerClass.owner)
            ) { "Generated serializer does not have constructor with required number of arguments" }
        } else {
            findConstructorWithoutTypeParameters(serializerClass, needToCopyAnnotations)
        }
        return ctor
    }

    private fun findConstructorWithoutTypeParameters(
        serializerClass: IrClassSymbol,
        needToCopyAnnotations: Boolean,
    ): IrConstructorSymbol {
        val constructors = serializerClass.constructors
        // search for new signature of polymorphic/sealed/contextual serializer
        return if (!needToCopyAnnotations) {
            constructors.single { it.owner.isPrimary }
        } else {
            constructors.find { it.owner.lastArgumentIsAnnotationArray() }
                ?: generator.runtimeTooLowError()
        }
    }

    context(irBuilder: IrBuilderWithScope)
    private fun classReferenceOf(kType: IrSimpleType): IrClassReference = generator.classReference(kType.classOrUpperBound()!!)

    context(irBuilder: IrBuilderWithScope) private fun argsForContextSerializer(
        kType: IrSimpleType,
        genericIndex: Int?,
        typeArgumentsAsTypes: List<IrType>,
    ): Args? {
        // don't create an instance if the serializer is being created for the cache
        if (genericIndex == null && kType.genericIndex != null) {
            // if context serializer parametrized by generic type (kType.genericIndex != null)
            // and generic types are not allowed (always genericIndex == null for cache)
            // then serializer can't be cached
            return null
        }
        val typeArgs = listOf(kType)
        // modern runtimes always have new signature of context serializer:
        //    serializableClass: KClass<T>,
        //    fallbackSerializer: KSerializer<T>?,
        //    typeArgumentsSerializers: Array<KSerializer<*>>
        val args = buildList<IrExpression> {
            add(classReferenceOf(kType))

            val fallbackDefaultSerializer =
                findTypeSerializer(compilerContext, kType).takeIf { it?.owner?.classId != contextSerializerId }
            add(instantiate(fallbackDefaultSerializer, kType) ?: irBuilder.irNull())

            add(
                generator.createArrayOfExpression(
                    generator.wrapIrTypeIntoKSerializerIrType(kType, variance = Variance.OUT_VARIANCE),
                    typeArgumentsAsTypes.map {
                        val argSer = with(generator) {
                            findTypeSerializerOrContext(it)
                        }
                        instantiate(argSer, it) ?: return null
                    })
            )
        }
        return Args(args, typeArgs)
    }

    context(irBuilder: IrBuilderWithScope) private fun instantiateEnumSerializer(serializerClass: IrClassSymbol, kType: IrSimpleType): IrExpression {
        val enumDescriptor = kType.classOrNull!!
        val typeArgs = listOf(kType)
        // instantiate serializer only inside enum Companion
        if (generator !is SerializableCompanionIrGenerator) {
            // otherwise call Companion.serializer()
            generator.callSerializerFromCompanion(kType, typeArgs, emptyList(), enumSerializerId)?.let { return it }
        }

        val enumArgs = mutableListOf(
            irBuilder.irString(kType.serialName()),
            irBuilder.irCall(enumDescriptor.owner.findEnumValuesMethod()),
        )

        if (!hasEnumFactories) return instantiateLegacyEnumSerializer(serializerClass, enumArgs, typeArgs)

        // runtime contains enum serializer factory functions
        val factoryFunc: IrSimpleFunctionSymbol = if (enumDescriptor.owner.isEnumWithSerialInfoAnnotation()) {
            // need to store SerialInfo annotation in descriptor
            val enumEntries = enumDescriptor.owner.enumEntries()
            val entriesNames = enumEntries.map { it.annotations.serialNameValue?.let { n -> irBuilder.irString(n) } ?: irBuilder.irNull() }
            val entriesAnnotations = enumEntries.map {
                val annotationsConstructors = generator.copyAnnotationsFrom(it.annotations)
                if (annotationsConstructors.isEmpty()) {
                    irBuilder.irNull()
                } else {
                    generator.createArrayOfExpression(compilerContext.irBuiltIns.annotationType, annotationsConstructors)
                }
            }

            val classAnnotationsConstructors = generator.copyAnnotationsFrom(enumDescriptor.owner.annotations)
            val classAnnotations = if (classAnnotationsConstructors.isEmpty()) {
                irBuilder.irNull()
            } else {
                generator.createArrayOfExpression(compilerContext.irBuiltIns.annotationType, classAnnotationsConstructors)
            }
            val annotationArrayType =
                compilerContext.irBuiltIns.arrayClass.typeWith(compilerContext.irBuiltIns.annotationType.makeNullable())

            enumArgs += generator.createArrayOfExpression(compilerContext.irBuiltIns.stringType.makeNullable(), entriesNames)
            enumArgs += generator.createArrayOfExpression(annotationArrayType, entriesAnnotations)
            enumArgs += classAnnotations

            compilerContext.annotatedEnumSerializerFactoryFunc!!
        } else {
            compilerContext.enumSerializerFactoryFunc!!
        }

        val factoryReturnType = factoryFunc.owner.returnType.substitute(factoryFunc.owner.typeParameters, typeArgs)
        return generator.irInvoke(factoryFunc, enumArgs, typeArgs, factoryReturnType)
    }

    context(irBuilder: IrBuilderWithScope)
    private fun instantiateLegacyEnumSerializer(
        serializerClass: IrClassSymbol,
        enumArgs: List<IrExpression>,
        typeArgs: List<IrSimpleType>,
    ): IrExpression {
        assert(serializerClass.owner.classId == enumSerializerId) { "Expected enum serializer, got $serializerClass" }
        val ctor = findConstructorWithoutTypeParameters(serializerClass, needToCopyAnnotations = false).owner
        return callConstructor(ctor, typeArgs, enumArgs)
    }

    context(irBuilder: IrBuilderWithScope)
    private fun regularArgs(typeArgumentsAsTypes: List<IrType>): Args? {
        val args = typeArgumentsAsTypes.map {
            val argSer = generator.findTypeSerializerOrContext(
                it
            )
            instantiate(argSer, it) ?: return null
        }
        return Args(args, typeArgumentsAsTypes)
    }

    context(irBuilder: IrBuilderWithScope) private fun instantiateSealedSerializer(
        serializerClass: IrClassSymbol,
        sealedType: IrSimpleType,
    ): IrExpression {
        val needToCopyAnnotations = true
        // for sealed serializer the parent sealed serializable type is always the first type argument
        val typeArgs = listOf(sealedType)

        // If can call from companion:
        if (serializerClass == sealedType.classOrUpperBound()?.owner.classSerializer(compilerContext) && generator !is SerializableCompanionIrGenerator) {
            val args = sealedType.arguments.map { typeArgOfSealedClass ->
                val irTypeArgOfSealedClass = typeArgOfSealedClass.typeOrNull
                when {
                    irTypeArgOfSealedClass?.isTypeParameter() == true && rootSerializableClass != null -> {
                        /*
                         * Since we don't use the serializers cache here, we can use genericIndex property, and it matches the current generic context
                         */
                        val genericIndex = irTypeArgOfSealedClass.genericIndex
                        serializerInstance(
                            null,
                            irTypeArgOfSealedClass,
                            genericIndex,
                        ) ?: irBuilder.irGetObject(compilerContext.unitSerializerClass!!)
                    }

                    irTypeArgOfSealedClass != null && !irTypeArgOfSealedClass.isTypeParameter() -> {
                        // create serializer for class type argument
                        val serializer = generator.findTypeSerializerOrContext(irTypeArgOfSealedClass)
                        serializerInstance(
                            serializer,
                            irTypeArgOfSealedClass,
                            null,
                        ) ?: irBuilder.irGetObject(compilerContext.unitSerializerClass!!)
                    }

                    else -> {
                        // for star projection we can't pick serializer so use Unit serializer
                        // do the same in other unknown cases
                        irBuilder.irGetObject(compilerContext.unitSerializerClass!!)
                    }
                }
            }
            generator.callSerializerFromCompanion(sealedType, typeArgs, args, sealedSerializerId)?.let { return it }
        }


        val args = mutableListOf<IrExpression>().apply {
            add(irBuilder.irString(sealedType.serialName()))
            add(classReferenceOf(sealedType))
            val [subclasses, subSerializers] = generator.allSealedSerializableSubclassesFor(
                sealedType.classOrUpperBound()!!.owner,
                compilerContext
            )
            val projectedOutCurrentKClass =
                compilerContext.irBuiltIns.kClassClass.typeWithArguments(
                    listOf(makeTypeProjection(sealedType, Variance.OUT_VARIANCE))
                )
            add(
                generator.createArrayOfExpression(
                    projectedOutCurrentKClass,
                    subclasses.map { classReferenceOf(it) }
                )
            )
            add(
                generator.createArrayOfExpression(
                    generator.wrapIrTypeIntoKSerializerIrType(sealedType, variance = Variance.OUT_VARIANCE),
                    subSerializers.mapIndexed { i, subclassSerializer ->
                        val subclassType = subclasses[i]

                        val supertypePath = if (sealedType.arguments.isNotEmpty()) findSupertypePath(subclassType, sealedType) else emptyList()

                        val expr = instantiateWithNewGetter(
                            subclassSerializer,
                            subclassType,
                            subclassType.genericIndex,
                        ) { indexInSubtype, genericType ->
                            getGenericGetterForSubclassOfSealed(sealedType, subclassType, supertypePath, indexInSubtype, genericType)
                        }!!
                        val substitutedTypeArgumentsOfSubclass = getSubstitutedTypeArguments(subclassType, sealedType, supertypePath)
                        expr.type = expr.type.substitute(subclassType.getClass()!!.typeParameters, substitutedTypeArgumentsOfSubclass)

                        // if the expression is a function call, then we should substitute type arguments for this invoke (like call of `serializer<A, B, ...>()`)
                        if (expr is IrFunctionAccessExpression && expr.typeArguments.size == substitutedTypeArgumentsOfSubclass.size) {
                            expr.typeArguments.indices.forEach { expr.typeArguments[it] = substitutedTypeArgumentsOfSubclass[it] }
                        }
                        generator.wrapWithNullableSerializerIfNeeded(expr.type, expr, nullableSerClass)
                    }
                )
            )
        }
        val newArgs = addAnnotationsToArgs(sealedType, args)

        val ctor = findConstructorWithoutTypeParameters(serializerClass, needToCopyAnnotations).owner
        return callConstructor(ctor, typeArgs, newArgs)
    }

    /**
     * Returns a serializer expression for a type parameter of a sealed subclass.
     *
     * Maps [indexInSubtype] to the corresponding argument of [sealedType] through
     * [supertypePath]. If that argument belongs to the current generic context,
     * delegates to [genericGetter]; if it is concrete, creates its serializer directly.
     * Parameters not propagated to the sealed supertype fall back to a polymorphic
     * serializer for their representative upper bound.
     */
    context(irBuilder: IrBuilderWithScope)
    private fun getGenericGetterForSubclassOfSealed(
        sealedType: IrSimpleType,
        subclassType: IrSimpleType,
        supertypePath: List<IrSimpleType>,
        indexInSubtype: Int,
        genericType: IrType,
    ): IrExpression {
        // get actual type argument index in sealed class and take its index in the context of current genericGetter
        val indexInSealedClass = findIndexInParent(indexInSubtype, supertypePath)
        val typeArgInSealedClass = indexInSealedClass?.let { sealedType.arguments.getOrNull(it)?.typeOrNull }
        val genericIndex = typeArgInSealedClass?.genericIndex

        return when {
            typeArgInSealedClass != null && !typeArgInSealedClass.isTypeParameter() -> {
                // serializer for type argument of sealed class if it is not type parameter itself
                val serializer = generator.findTypeSerializerOrContext(typeArgInSealedClass)
                serializerInstance(serializer, typeArgInSealedClass, null)!!
            }
            genericGetter != null && genericIndex != null -> {
                // Generic type argument of sealed class
                // Copy the expression because the same IR node must not be reused under multiple parents
                genericGetter.invoke(genericIndex, genericType).deepCopyWithSymbols()
            }
            !genericType.isTypeParameter() -> {
                // non-generic type argument of subclass
                val serializer = generator.findTypeSerializerOrContext(subclassType)
                serializerInstance(
                    serializer,
                    subclassType,
                    null,
                )!!
            }
            else -> {
                serializerInstance(
                    compilerContext.finderForBuiltins().findClass(polymorphicSerializerId),
                    (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound
                )!!
            }
        }
    }

    /**
     * Returns type arguments of [subclassType] expressed in the [sealedType] context.
     * Parameters not propagated to the sealed supertype are replaced with their representative upper bounds.
     */
    private fun getSubstitutedTypeArguments(
        subclassType: IrSimpleType,
        sealedType: IrSimpleType,
        supertypePath: List<IrSimpleType>,
    ): List<IrType> {
        val subclass = subclassType.getClass() ?: return emptyList()
        val substitutions = sealedType.argumentTypesOrUpperBounds()
        return subclass.typeParameters.map {
            findIndexInParent(it.index, supertypePath)?.let(substitutions::getOrNull) ?: it.representativeUpperBound
        }
    }

    context(irBuilder: IrBuilderWithScope)
    fun instantiateWithNewGetter(
        serializerClass: IrClassSymbol?,
        kType: IrType,
        genericIndex: Int?,
        genericGetter: ((Int, IrType) -> IrExpression)?,
    ): IrExpression? {
        return Instantiator(generator, compilerContext, rootSerializableClass, genericGetter).serializerInstance(serializerClass, kType, genericIndex)
    }


    context(irBuilder: IrBuilderWithScope)
    private fun instantiate(serializer: IrClassSymbol?, type: IrType): IrExpression? {
        val expr = serializerInstance(
            serializer,
            type,
            type.genericIndex,
        ) ?: return null
        return generator.wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
    }

    context(irBuilder: IrBuilderWithScope) private fun getSingletonSerializer(
        serializerClassOriginal: IrClassSymbol,
        kType: IrType,
    ): IrExpression? {
        val serializerClass = serializerClassOriginal.owner
        // rootSerializableClass is null only if we are compiling serializer getter
        //   In this case, the private serializer will always be located in the same package, otherwise a syntax error will occur.
        //   If it is not null, rootSerializableClass.getPackageFragment() will return IrFile we are currently compiling
        val sameFileAccess = serializerClass.getPackageFragment() == rootSerializableClass?.getPackageFragment()
        return if (rootSerializableClass == null || serializerClass.visibility != DescriptorVisibilities.PRIVATE || sameFileAccess) {
            // we can access the serializer object directly only if it is not private, or is located in the same file as the class using it
            irBuilder.irGetObject(serializerClassOriginal)
        } else {
            val simpleType = (kType as? IrSimpleType) ?: error("Don't know how to work with type ${kType.classFqName}")

            if (simpleType.getClass()?.isObject == true) {
                generator.callSerializerFromObject(simpleType, emptyList())
                    ?: error("Can't get serializer from 'serializer()' function for object ${kType.classFqName}")
            } else {
                generator.callSerializerFromCompanion(simpleType, emptyList(), emptyList(), serializerClassOriginal.owner.classId)
                    ?: error("Can't get serializer from companion's 'serializer()' function for type ${kType.classFqName}")
            }
        }
    }

}
