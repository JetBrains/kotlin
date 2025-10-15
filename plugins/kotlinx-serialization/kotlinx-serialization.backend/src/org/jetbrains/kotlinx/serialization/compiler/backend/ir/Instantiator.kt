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
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
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
import kotlin.collections.orEmpty

internal class Instantiator(
    val generator: BaseIrGenerator,
    val compilerContext: SerializationPluginContext,
    val rootSerializableClass: IrClass? = null,
    val genericGetter: ((Int, IrType) -> IrExpression)? = null,
) {
    val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()

    init {
        val hasNewCtxSerCtor = compilerContext.referenceConstructors(contextSerializerId)
            .any { it.owner.hasShape(regularParameters = 3) }

        val hasEnumFactories =
            compilerContext.enumSerializerFactoryFunc != null && compilerContext.annotatedEnumSerializerFactoryFunc != null

        assert(hasNewCtxSerCtor && hasEnumFactories) { "Your serialization runtime is pre-historical, bruh" }
    }

    data class Args(
        val args: List<IrExpression>, val typeArgs: List<IrType>,
        val useCompanionShortcut: Boolean, val needToCopyAnnotations: Boolean,
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

        val args = when (serializerClass.owner.classId) {
            polymorphicSerializerId -> Args(listOf(classReferenceOf(kType)), listOf(kType), false, true)
            contextSerializerId -> {
                doContextSerializer(kType, genericIndex, typeArgumentsAsTypes) ?: return null
            }
            objectSerializerId -> Args(
                args = listOf(irBuilder.irString(kType.serialName()), irBuilder.irGetObject(kType.classOrUpperBound()!!)),
                typeArgs = listOf(kType),
                useCompanionShortcut = false,
                needToCopyAnnotations = true,
            )
            sealedSerializerId -> doSealed(serializerClass, kType)
            enumSerializerId -> return instantiateEnumSerializer(kType)
            referenceArraySerializerId -> {
                val (origArgs, origTypeArgs) = regularArgs(typeArgumentsAsTypes) ?: return null
                val args = listOf(generator.wrapperClassReference(typeArgumentsAsTypes.single())) + origArgs
                val typeArgs = listOf(origTypeArgs[0].makeNotNull()) + origTypeArgs
                Args(args, typeArgs, false, false)
            }
            else -> regularArgs(typeArgumentsAsTypes) ?: return null
        }

        val canUseShortcut =
            !kType.isInterface() && serializerClass == kType.classOrUpperBound()?.owner.classSerializer(compilerContext) && generator !is SerializableCompanionIrGenerator
        if (canUseShortcut || args.useCompanionShortcut) {
            // This is default type serializer, we can shortcut through Companion.serializer()
            // BUT not during generation of this method itself
            // TODO: check if we still want to build args for sealed/polymorphic serializers here
            generator.callSerializerFromCompanion(kType, args.typeArgs, args.args, serializerClass.owner.classId)?.let { return it }
        }


        val needToCopyAnnotations = args.needToCopyAnnotations

        val ctor = findConstructor(serializerClass, needToCopyAnnotations)
        val ctorDecl = ctor.owner

        val newArgs = if (needToCopyAnnotations) {
            val classAnnotations =
                generator.copyAnnotationsFrom(kType.getClass()?.let { generator.collectSerialInfoAnnotations(it) }.orEmpty())
            args.args + generator.createArrayOfExpression(compilerContext.irBuiltIns.annotationType, classAnnotations)
        } else args.args

        val typeParameters = ctorDecl.parentAsClass.typeParameters
        val substitutedReturnType = ctorDecl.returnType.substitute(typeParameters, args.typeArgs)
        return generator.irInvoke(
            ctor,
            // User may declare serializer with fixed type arguments, e.g. class SomeSerializer : KSerializer<ClosedRange<Float>>
            arguments = newArgs.takeIf { it.size == ctorDecl.parameters.size }.orEmpty(),
            typeArguments = args.typeArgs.takeIf { it.size == ctorDecl.typeParameters.size }.orEmpty(),
            returnTypeHint = substitutedReturnType
        )
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
            constructors.find { it.owner.lastArgumentIsAnnotationArray() } ?: error("Your serialization runtime is pre-historical, bruh")
        }
    }

    context(irBuilder: IrBuilderWithScope)
    private fun classReferenceOf(kType: IrSimpleType): IrClassReference = generator.classReference(kType.classOrUpperBound()!!)

    context(irBuilder: IrBuilderWithScope) private fun doContextSerializer(
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
        return Args(args, typeArgs, false, false)
    }

    context(irBuilder: IrBuilderWithScope) private fun instantiateEnumSerializer(kType: IrSimpleType): IrExpression {
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
    private fun regularArgs(typeArgumentsAsTypes: List<IrType>): Args? {
        val args = typeArgumentsAsTypes.map {
            val argSer = generator.findTypeSerializerOrContext(
                it
            )
            instantiate(argSer, it) ?: return null
        }
        return Args(args, typeArgumentsAsTypes, false, false)
    }

    context(irBuilder: IrBuilderWithScope) private fun doSealed(
        serializerClass: IrClassSymbol,
        kType: IrSimpleType,
    ): Args {
        val needToCopyAnnotations = true
        val typeArgs = listOf(kType)

        // If can call from companion:
        if (serializerClass == kType.classOrUpperBound()?.owner.classSerializer(compilerContext) && generator !is SerializableCompanionIrGenerator) {
            val args = kType.arguments.map { typeArg ->
                val type = typeArg.typeOrNull
                when {
                    type?.isTypeParameter() == true && rootSerializableClass != null -> {
                        // try to use type argument from root serializable class
                        val indexInRootClass = typeArg.indexInClass(rootSerializableClass)
                        serializerInstance(
                            null,
                            type,
                            indexInRootClass,
                        ) ?: irBuilder.irGetObject(compilerContext.unitSerializerClass!!)
                    }

                    type != null && !type.isTypeParameter() -> {
                        // create serializer for class type argument
                        val serializer = generator.findTypeSerializerOrContext(type)
                        serializerInstance(
                            serializer,
                            type,
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
            return Args(args, typeArgs, true, false)
            //                    generator.callSerializerFromCompanion(kType, typeArgs, args, sealedSerializerId)?.let { return TODO(it.toString()) }
        }


        val args = mutableListOf<IrExpression>().apply {
            add(irBuilder.irString(kType.serialName()))
            add(classReferenceOf(kType))
            val (subclasses, subSerializers) = generator.allSealedSerializableSubclassesFor(
                kType.classOrUpperBound()!!.owner,
                compilerContext
            )
            val projectedOutCurrentKClass =
                compilerContext.irBuiltIns.kClassClass.typeWithArguments(
                    listOf(makeTypeProjection(kType, Variance.OUT_VARIANCE))
                )
            add(
                generator.createArrayOfExpression(
                    projectedOutCurrentKClass,
                    subclasses.map { classReferenceOf(it) }
                )
            )
            add(
                generator.createArrayOfExpression(
                    generator.wrapIrTypeIntoKSerializerIrType(kType, variance = Variance.OUT_VARIANCE),
                    subSerializers.mapIndexed { i, serializer ->
                        val type = subclasses[i]

                        val path = if (kType.arguments.isNotEmpty()) findPath(type, kType) else null

                        val expr = instantiateWithNewGetter(
                            serializer,
                            type,
                            type.genericIndex,
                        ) { index, genericType ->
                            val indexInParent = path?.let { mapTypeParameterIndex(index, it) }

                            when {
                                genericGetter != null && indexInParent != null -> {
                                    genericGetter.invoke(indexInParent, genericType)
                                }
                                !genericType.isTypeParameter() -> {
                                    val serializer = generator.findTypeSerializerOrContext(type)
                                    serializerInstance(
                                        serializer,
                                        type,
                                        null,
                                    )!!
                                }
                                else -> {
                                    serializerInstance(
                                        compilerContext.referenceClass(polymorphicSerializerId),
                                        (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound
                                    )!!
                                }
                            }
                        }!!
                        generator.wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
                    }
                )
            )
        }
        return Args(args, typeArgs, false, needToCopyAnnotations)
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
