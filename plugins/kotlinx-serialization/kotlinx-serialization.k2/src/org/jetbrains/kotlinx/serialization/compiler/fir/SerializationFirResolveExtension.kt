/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIALIZER_FACTORY_INTERFACE_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.generatedSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.kSerializerId

class SerializationFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    internal val runtimeHasEnumSerializerFactory by lazy {
        val hasFactory = session.symbolProvider.getTopLevelCallableSymbols(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
        ).isNotEmpty()
        val hasMarkedFactory = session.symbolProvider.getTopLevelCallableSymbols(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
        ).isNotEmpty()
        hasFactory && hasMarkedFactory
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> {
        val result = mutableSetOf<Name>()
        with(session) {
            if (classSymbol.shouldHaveGeneratedMethodsInCompanion && !classSymbol.isSerializableObject)
                result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            if (classSymbol.shouldHaveGeneratedSerializer && !classSymbol.isInternallySerializableObject)
                result += SerialEntityNames.SERIALIZER_CLASS_NAME
        }

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

        val isExternalSerializer = classSymbol.isExternalSerializer
        when {
            classSymbol.isCompanion && !isExternalSerializer -> {
                result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
                val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
                if (origin?.key == SerializationPluginKey) {
                    result += SpecialNames.INIT
                }
            }

            classId.shortClassName == SerialEntityNames.SERIALIZER_CLASS_NAME -> {
                // TODO: check classSymbol for already added functions
                result += setOf(
                    SpecialNames.INIT,
                    SerialEntityNames.SAVE_NAME,
                    SerialEntityNames.LOAD_NAME,
                    SerialEntityNames.SERIAL_DESC_FIELD_NAME
                )
                if (classSymbol.resolvedSuperTypes.any { it.classId == generatedSerializerId }) {
                    result += SerialEntityNames.CHILD_SERIALIZERS_GETTER

                    if (classSymbol.typeParameterSymbols.isNotEmpty()) {
                        result += SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER
                    }
                }
            }

            isExternalSerializer -> {
                if (classSymbol.declarationSymbols.filterIsInstance<FirPropertySymbol>()
                        .none { it.name == SerialEntityNames.SERIAL_DESC_FIELD_NAME }
                ) {
                    result += SerialEntityNames.SERIAL_DESC_FIELD_NAME
                }

                if (classSymbol.isCompanion) result += SerialEntityNames.SERIALIZER_PROVIDER_NAME

                if (classSymbol.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>()
                        .none { it.name == SerialEntityNames.SAVE_NAME }
                ) {
                    result += SerialEntityNames.SAVE_NAME // TODO how check parameters?!
                }
                if (classSymbol.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>()
                        .none { it.name == SerialEntityNames.LOAD_NAME }
                ) {
                    result += SerialEntityNames.LOAD_NAME // TODO how check parameters?!
                }
            }

            with(session) { classSymbol.isSerializableObject } -> result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
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
        return targets.singleOrNull() ?: error("Multiple overrides found for ${callableId.callableName}")
    }

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.callableName == SerialEntityNames.SERIALIZER_PROVIDER_NAME) {
            val serializableClass = if (owner.isCompanion) {
                val containingSymbol = owner.getContainingDeclarationSymbol(session) as? FirClassSymbol<*> ?: return emptyList()
                if (with(session) { containingSymbol.shouldHaveGeneratedMethodsInCompanion }) containingSymbol else null
            } else {
                if (with(session) { owner.isSerializableObject }) owner else null
            }

            serializableClass ?: return emptyList()
            return listOf(generateSerializerGetterInCompanion(owner, serializableClass, callableId))
        }
        if (!owner.isSerializer) return emptyList()
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

    private fun generateSerializerGetterInCompanion(
        owner: FirClassSymbol<*>,
        serializableClassSymbol: FirClassSymbol<*>,
        callableId: CallableId
    ): FirNamedFunctionSymbol {
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

            typeParameters.addAll(serializableClassSymbol.typeParameterSymbols.map { newSimpleTypeParameter(session, symbol, it.name) })
            val parametersAsArguments = typeParameters.map { it.toConeType() }.toTypedArray<ConeTypeProjection>()

            valueParameters.addAll(List(serializableClassSymbol.typeParameterSymbols.size) { i ->
                newSimpleValueParameter(
                    session,
                    kSerializerId.constructClassLikeType(arrayOf(parametersAsArguments[i]), false).toFirResolvedTypeRef(),
                    Name.identifier("${SerialEntityNames.typeArgPrefix}$i")
                )
            })


            returnTypeRef = buildResolvedTypeRef {
                type = kSerializerId.constructClassLikeType(
                    arrayOf(serializableClassSymbol.constructType(parametersAsArguments, false)),
                    isNullable = false
                )
            }
        }
        return f.symbol
    }

    @OptIn(SymbolInternals::class)
    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (!owner.isSerializer) return emptyList()
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
        session.predicateBasedProvider.getSymbolsByPredicate(FirSerializationPredicates.annotatedWithSerializableOrMeta)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner
        val defaultObjectConstructor = buildPrimaryConstructor(
            owner, isInner = false, SerializationPluginKey, status = FirResolvedDeclarationStatusImpl(
                Visibilities.Private,
                Modality.FINAL,
                EffectiveVisibility.PrivateInClass
            )
        )
        if (owner.name == SerialEntityNames.SERIALIZER_CLASS_NAME && owner.typeParameterSymbols.isNotEmpty()) {
            val parameterizedConstructor = buildConstructor {
                moduleData = session.moduleData
                origin = SerializationPluginKey.origin
                returnTypeRef = defaultObjectConstructor.returnTypeRef
                symbol = FirConstructorSymbol(owner.classId)
                dispatchReceiverType = defaultObjectConstructor.dispatchReceiverType
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Private,
                    Modality.FINAL,
                    EffectiveVisibility.PrivateInFile // accessed from a companion
                )
                valueParameters.addAll(owner.typeParameterSymbols.mapIndexed { i, typeParam ->
                    newSimpleValueParameter(
                        session,
                        kSerializerId.constructClassLikeType(arrayOf(typeParam.toConeType()), false).toFirResolvedTypeRef(),
                        Name.identifier("${SerialEntityNames.typeArgPrefix}$i")
                    )
                })
            }
            return listOf(defaultObjectConstructor.symbol, parameterizedConstructor.symbol)
        }
        return listOf(defaultObjectConstructor.symbol)
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
                newSimpleTypeParameter(session, symbol, param.name)
            })

            val parametersAsArguments = typeParameters.map { it.toConeType() }.toTypedArray<ConeTypeProjection>()
            superTypeRefs += generatedSerializerId.constructClassLikeType(
                arrayOf(
                    owner.constructType(
                        parametersAsArguments,
                        isNullable = false
                    )
                ), isNullable = false
            ).toFirResolvedTypeRef()
        }
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
            if (with(session) { owner.companionNeedsSerializerFactory }) {
                val serializerFactoryClassId = ClassId(SerializationPackages.internalPackageFqName, SERIALIZER_FACTORY_INTERFACE_NAME)
                superTypeRefs += serializerFactoryClassId.constructClassLikeType(emptyArray(), false).toFirResolvedTypeRef()
            }
        }
        return regularClass.symbol
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirSerializationPredicates.annotatedWithSerializableOrMeta, FirSerializationPredicates.hasMetaAnnotation)
    }

    private val FirClassSymbol<*>.isSerializer: Boolean
        get() = name == SerialEntityNames.SERIALIZER_CLASS_NAME || isExternalSerializer

    private val FirClassSymbol<*>.isExternalSerializer: Boolean
        get() = session.predicateBasedProvider.matches(FirSerializationPredicates.serializerFor, this)

    context(FirSession)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val FirClassSymbol<*>.companionNeedsSerializerFactory: Boolean
        get() {
            if (!(moduleData.platform.isNative() || moduleData.platform.isJs())) return false
            if (isSerializableObject) return true
            if (isSerializableEnum) return true
            if (isAbstractOrSealedSerializableClass) return true
            if (isSealedSerializableInterface) return true
            if (typeParameterSymbols.isEmpty()) return false
            return true
        }

}
