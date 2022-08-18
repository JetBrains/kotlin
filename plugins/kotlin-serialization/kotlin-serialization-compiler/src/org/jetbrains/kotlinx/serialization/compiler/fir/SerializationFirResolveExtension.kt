/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
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
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

object SerializationPluginKey : GeneratedDeclarationKey() {
    override fun toString(): String {
        return "KotlinxSerializationPlugin"
    }
}

val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)
val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)

class SerializationFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion && !classSymbol.isSerializableObject)
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        if (classSymbol.shouldHaveGeneratedSerializer /* TODO && !classSymbol.hasCompanionObjectAsSerializer*/)
            result += SerialEntityNames.SERIALIZER_CLASS_NAME
        return result
    }

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
                // TODO: check classSymbol for already added functions
                // TODO: support user-defined serializers?
                result += setOf(
                    SpecialNames.INIT,
                    SerialEntityNames.SAVE_NAME,
                    SerialEntityNames.LOAD_NAME,
                    SerialEntityNames.SERIAL_DESC_FIELD_NAME
                )
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

    @OptIn(SymbolInternals::class)
    private fun <T> getFromSupertype(callableId: CallableId, owner: FirClassSymbol<*>, extractor: (FirTypeScope) -> List<T>): T {
        val scopeSession = ScopeSession()
        val scopes = lookupSuperTypes(
            owner, lookupInterfaces = true, deep = false, useSiteSession = session
        ).mapNotNull { useSiteSuperType ->
            useSiteSuperType.scopeForSupertype(session, scopeSession, owner.fir)
        }
        val targets = scopes.flatMap { extractor(it) }
        val target = targets.singleOrNull() ?: error("Multiple overrides found for ${callableId.callableName}")
        return target
    }

    // TODO: support @Serializer(for)
    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (owner.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT && callableId.callableName == SerialEntityNames.SERIALIZER_PROVIDER_NAME)
            return listOf(generateSerializerGetterInCompanion(owner, callableId))
        if (owner.name != SerialEntityNames.SERIALIZER_CLASS_NAME) return emptyList()
        if (callableId.callableName !in setOf(
                SpecialNames.INIT,
                SerialEntityNames.SAVE_NAME,
                SerialEntityNames.LOAD_NAME,
                SerialEntityNames.CHILD_SERIALIZERS_GETTER,
                SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER
            )
        ) return emptyList()
        val target = getFromSupertype(callableId, owner) { it.getFunctions(callableId.callableName) }
        val original = target.fir
        val copy = buildSimpleFunctionCopy(original) {
            symbol = FirNamedFunctionSymbol(callableId)
            origin = SerializationPluginKey.origin
            status = original.status.copy(modality = Modality.FINAL)

        }
        return listOf(copy.symbol)
    }

    private fun generateSerializerGetterInCompanion(owner: FirClassSymbol<*>, callableId: CallableId): FirNamedFunctionSymbol {
        val f = buildSimpleFunction {
            moduleData = session.moduleData
            symbol = FirNamedFunctionSymbol(callableId)
            origin = SerializationPluginKey.origin
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            name = callableId.callableName
            dispatchReceiverType = owner.defaultType()

            // TODO: handle serializable objects
            val serializableType =
                (owner.getContainingDeclarationSymbol(session) as? FirClassSymbol<*>) ?: error("Can't get outer class for $owner")

            // TODO: Add value parameters & type parameters for parameterized classes
            returnTypeRef = buildResolvedTypeRef {
                type = kSerializerClassId.constructClassLikeType(
                    arrayOf(serializableType.defaultType().toTypeProjection(Variance.INVARIANT)),
                    isNullable = false
                )
            }
        }
        return f.symbol
    }

    @OptIn(SymbolInternals::class) // TODO: localSerializersFieldDescriptors
    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (owner.name != SerialEntityNames.SERIALIZER_CLASS_NAME) return emptyList()
        if (callableId.callableName != SerialEntityNames.SERIAL_DESC_FIELD_NAME) return emptyList()

        val target = getFromSupertype(callableId, owner) { it.getProperties(callableId.callableName).filterIsInstance<FirPropertySymbol>() }
        val original = target.fir
        val copy = buildPropertyCopy(original) {
            symbol = FirPropertySymbol(callableId)
            origin = SerializationPluginKey.origin
            status = original.status.copy(modality = Modality.FINAL)
            getter = buildPropertyAccessor {
                status = original.status.copy(modality = Modality.FINAL)
                symbol = FirPropertyAccessorSymbol()
                origin = SerializationPluginKey.origin
                moduleData = session.moduleData
                isGetter = true
                returnTypeRef = original.returnTypeRef
                dispatchReceiverType = owner.defaultType()
                propertySymbol = this@buildPropertyCopy.symbol
            }
        }
        return listOf(copy.symbol)
    }

    // FIXME: it seems that this list will always be used, why not provide it automatically?
    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(FirSerializationPredicates.annotatedWithSerializable)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructor = buildConstructor(context.owner.classId, isInner = false, SerializationPluginKey)
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
            // TODO: add deprecate hidden
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
                type = generatedSerializerClassId.constructClassLikeType(arrayOf(owner.constructStarProjectedType()), isNullable = false)
            }
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
        register(FirSerializationPredicates.annotatedWithSerializable)
    }
}