/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.AnnotatedWith
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

object SerializationPluginKey : FirPluginKey() {
    override fun toString(): String {
        return "KotlinxSerializationPlugin"
    }
}

val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)
val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)

class SerializationFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    // FIXME: I can't check status here
    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion && !classSymbol.isSerializableObject)
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        if (classSymbol.shouldHaveGeneratedSerializer /* TODO && !classSymbol.hasCompanionObjectAsSerializer*/)
            result += SerialEntityNames.SERIALIZER_CLASS_NAME
        return result
    }
    /**
     * It's not documented how these classes relate with getCallableNamesForClass
     * Document says:
     * 1.> If you generate some class using generateClassLikeDeclaration then you don't need to fill it's declarations.
     * Q: Don't need or MUSTN'T?
     *
     * 2. > Instead of that you need to generate members via generateProperties/Functions/Constructors methods
     * of same generation extension (this is important note if you have multiple generation extensions in your plugin)
     *
     * Q: Does this mean that for e.g. generated companion `generateFunctions` will be called regardless of provided predicate?
     *
     * 3. This method is stated to be called on SUPERTYPES, but getNestedClassifiersNames stated to be called after SUPERTYPES. How is this possible?
     *
     * 4. It is should be mentioned that supertypes for generated class should be added here,
     * while supertypes for exisiting classes — in separate extension
     */
    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        return when (classId.shortClassName) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> {
                generateCompanionDeclaration(classId)
            }
            SerialEntityNames.SERIALIZER_CLASS_NAME -> {
                addSerializerImplClass(classId)
            }
            else -> error("Can't generate class ${classId.asSingleFqName()}")
        }
    }

    /**
     * 1. It is unclear that for `generateConstructors` to work, one need to return SpecialNames.INIT from here
     * 1a. Also it is unclear that objects need constructors too.
     *
     * 2. If it is a user-written companion object, will I get here? Why?
     *
     * 3. Is it supposed to check that classSymbol does not already have same Name? How to add a generated overload?
     *
     */
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        val classId = classSymbol.classId
        val result = mutableSetOf<Name>()
        when (classId.shortClassName) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> {
                val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
                if (origin?.key == SerializationPluginKey) {
                    result += SpecialNames.INIT
                    result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
                } else {
                    // TODO: handle user-written companions & named companions
                }
            }
            SerialEntityNames.SERIALIZER_CLASS_NAME -> {
                //
                // TODO: check classSymbol for already added functions
                // TODO: support user-defined serializers?
                result += setOf(SpecialNames.INIT, SerialEntityNames.SAVE_NAME, SerialEntityNames.LOAD_NAME)
                if (classSymbol.superConeTypes.any {
                        it.classId == ClassId(
                            SerializationPackages.internalPackageFqName,
                            SerialEntityNames.GENERATED_SERIALIZER_CLASS
                        )
                    }) {
                    result += SerialEntityNames.CHILD_SERIALIZERS_GETTER

                    if (classSymbol.typeParameterSymbols.isNotEmpty()) {
                        result += SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER
                    }
                }
            }
            else -> if (classSymbol.isSerializableObject) result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
        }
        return result
    }

    // FIXME: it seems tedious to declare overrides. In old FE, one could do getMemberScope(typeParam) and get correct FO to simply copy data from it.
    //  Is this possible here? (see KSerializerDescriptorResolver.doCreateSerializerFunction)
    //
    //  lookupSuperTypes(owner, true, true, session).map { it.fullyExpandedType(session) }
    //  does not create correct substitutions.
    //
    // TODO: support @Serializer(for)
    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        if (owner == null) return emptyList()
        if (owner.name != SerialEntityNames.SERIALIZER_CLASS_NAME) return emptyList() // TODO: support other cases
//        val serializableClass = matchedClasses.firstOrNull { it.classId == owner.classId.outerClassId }
//        val (superTypeGenerated) = owner.resolvedSuperTypeRefs // TODO: more reliable logic than based on indices
//        val superGeneratedSerializerClass = superTypeGenerated.toRegularClassSymbol(session)!!
        println(owner)
        println(callableId)
//        when(callableId.callableName) {
//            SerialEntityNames.SAVE_NAME ->
//        }
        return emptyList()
    }

    // FIXME: it seems that this list will always be used, why not provide it automatically?
    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(AnnotatedWith(setOf(SerializationAnnotations.serializableAnnotationFqName)))
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateConstructors(owner: FirClassSymbol<*>): List<FirConstructorSymbol> {
        val constructor = buildConstructor(owner.classId, isInner = false, SerializationPluginKey)
        return listOf(constructor.symbol)
    }

    fun addSerializerImplClass(
        classId: ClassId
    ): FirClassLikeSymbol<*>? {
        val owner = matchedClasses.firstOrNull { it.classId == classId.outerClassId } ?: return null
        val hasTypeParams = owner.typeParameterSymbols.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerFirClass = buildRegularClass {
            moduleData = session.moduleData
            origin = SerializationPluginKey.origin
            classKind = serializerKind
            scopeProvider = session.kotlinScopeProvider
            name = SerialEntityNames.SERIALIZER_CLASS_NAME
            symbol = FirRegularClassSymbol(classId)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            // FIXME: how to create annotations?
//            annotations = listOf(Annotations.create(listOf(KSerializerDescriptorResolver.createDeprecatedHiddenAnnotation(thisDescriptor.module))))


            typeParameters.addAll(owner.typeParameterSymbols.map { param ->
                buildTypeParameter {
                    moduleData = session.moduleData
                    origin = SerializationPluginKey.origin
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    variance = Variance.INVARIANT
                    name = param.name
                    symbol = FirTypeParameterSymbol()
                    containingDeclarationSymbol = this@buildRegularClass.symbol
                    isReified = false
                    addDefaultBoundIfNecessary() // there should be KSerializer but whatever
                }
            })

            superTypeRefs += buildResolvedTypeRef {
                // FIXME: document how one gets type ref from FirClass
                //  is this a correct one or should I go with buildTypeProjectionWithVariance ?
                //  is this a correct place to add supertypes to synthetic declarations? Looks like FirSupertypesExtension does not see class.
                // It seems that this code generates KSerializer<Box<[declared type param of Box]>> instead of KSerializer<Box<*>>, but it didn't matter for old FE
                type = generatedSerializerClassId.constructClassLikeType(arrayOf(owner.defaultType().toTypeProjection(Variance.INVARIANT)), isNullable = false)
            }

            // FIXME: It seems that if I want to access correctly substituted supertype functions in generateFunctions (see comment there),
            //  I have to explicitly declare ALL supertypes, even though KSerializer, SerializationStrategy and DeserializationStrategy
            //  are a supertype to GeneratedSerializer by definition.

//            superTypeRefs += buildResolvedTypeRef {
//                type = kSerializerClassId.constructClassLikeType(arrayOf(owner.defaultType().toTypeProjection(Variance.INVARIANT)), isNullable = false)
//            }

        }
        // TODO: add typed constructor
//        val secondaryCtors =
//            if (!hasTypeParams)
//                emptyList()
//            else
//                listOf(
//                    KSerializerDescriptorResolver.createTypedSerializerConstructorDescriptor(
//                        serializerFirClass,
//                        thisDescriptor,
//                        typeParameters
//                    )
//                )
//        serializerFirClass.secondaryConstructors = secondaryCtors
        return serializerFirClass.symbol
    }

    fun generateCompanionDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        val owner = matchedClasses.firstOrNull { it.classId == classId.outerClassId } ?: return null
        if (owner.companionObjectSymbol != null) return null
        val regularClass = buildRegularClass {
            moduleData = session.moduleData
            origin = SerializationPluginKey.origin
            classKind = ClassKind.OBJECT
            scopeProvider = session.kotlinScopeProvider
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                isCompanion = true
            }
            name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            symbol = FirRegularClassSymbol(classId)
            superTypeRefs += session.builtinTypes.anyType
        }
        return regularClass.symbol
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        // No predicate on final/open etc
        register(AnnotatedWith(setOf(SerializationAnnotations.serializableAnnotationFqName)))
    }
}