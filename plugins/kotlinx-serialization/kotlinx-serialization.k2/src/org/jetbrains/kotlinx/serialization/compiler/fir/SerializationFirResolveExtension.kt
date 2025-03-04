/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIALIZER_FACTORY_INTERFACE_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.generatedSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.kSerializerId

@OptIn(DirectDeclarationsAccess::class)
class SerializationFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    internal val runtimeHasEnumSerializerFactory by lazy {
        val hasFactory = session.symbolProvider.getTopLevelCallableSymbols(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
        ).isNotEmpty()
        val hasAnnotatedFactory = session.symbolProvider.getTopLevelCallableSymbols(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
        ).isNotEmpty()
        hasFactory && hasAnnotatedFactory
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion(session) && !classSymbol.isSerializableObject(session))
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        if (classSymbol.shouldHaveGeneratedSerializer(session) && !classSymbol.isInternallySerializableObject(session))
            result += SerialEntityNames.SERIALIZER_CLASS_NAME

        return result
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol) return null
        if (!session.predicateBasedProvider.matches(FirSerializationPredicates.annotatedWithSerializableOrMeta, owner)) return null
        return when (name) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            SerialEntityNames.SERIALIZER_CLASS_NAME -> generateSerializerImplClass(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val classId = classSymbol.classId
        val result = mutableSetOf<Name>()

        val isExternalSerializer = classSymbol.isExternalSerializer
        when {
            classSymbol.isCompanion && !isExternalSerializer -> {
                result += SerialEntityNames.SERIALIZER_PROVIDER_NAME

                val containingClassSymbol = classSymbol.getContainingClassSymbol() as? FirClassSymbol
                if (containingClassSymbol != null && containingClassSymbol.keepGeneratedSerializer(session)) {
                    result += SerialEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME
                }
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

                if (classSymbol.isCompanion) {
                    result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
                    val containingClassSymbol = classSymbol.getContainingClassSymbol() as? FirClassSymbol
                    if (containingClassSymbol != null && containingClassSymbol.keepGeneratedSerializer(session)) {
                        result += SerialEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME
                    }
                }

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

            classSymbol.isSerializableObject(session) -> {
                result += SerialEntityNames.SERIALIZER_PROVIDER_NAME
                if (classSymbol.keepGeneratedSerializer(session)) {
                    result += SerialEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME
                }
            }
        }
        return result
    }

    @OptIn(SymbolInternals::class)
    private fun <T> getFromSupertype(callableId: CallableId, owner: FirClassSymbol<*>, extractor: (FirTypeScope) -> List<T>): T {
        val scopeSession = ScopeSession()
        val scopes = lookupSuperTypes(
            owner, lookupInterfaces = true, deep = false, useSiteSession = session
        ).mapNotNull { useSiteSuperType ->
            useSiteSuperType.scopeForSupertype(session, scopeSession, owner.fir, memberRequiredPhase = null)
        }
        val targets = scopes.flatMap { extractor(it) }
        return targets.singleOrNull() ?: error("Zero or multiple overrides found for ${callableId.callableName} in $owner")
    }

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.callableName == SerialEntityNames.SERIALIZER_PROVIDER_NAME || callableId.callableName == SerialEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME) {
            val serializableClass = if (owner.isSerializableObject(session)) {
                // regardless of whether this is a companion or regular object, using self
                // has priority over outer class (see COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS diagnostic
                // and serializableCompanion.kt test)
                owner
            } else if (owner.isCompanion) {
                val containingSymbol = owner.getContainingDeclaration(session) as? FirClassSymbol<*> ?: return emptyList()
                if (containingSymbol.shouldHaveGeneratedMethodsInCompanion(session)) containingSymbol else null
            } else null

            if (serializableClass == null) return emptyList()
            val serializableGetterInCompanion = generateSerializerGetterInCompanion(
                owner,
                serializableClass,
                callableId,
                callableId.callableName == SerialEntityNames.SERIALIZER_PROVIDER_NAME
            )
            val serializableGetterFromFactory =
                runIf(serializableClass.companionNeedsSerializerFactory(session) && callableId.callableName == SerialEntityNames.SERIALIZER_PROVIDER_NAME) {
                    val original = getFromSupertype(callableId, owner) { it.getFunctions(callableId.callableName) }.fir
                    generateSerializerFactoryVararg(owner, callableId, original)
                }
            return listOfNotNull(serializableGetterInCompanion, serializableGetterFromFactory)
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

        // TODO(KT-73060): To avoid an exception caused by the lazy resolution on the generated function with `FirResolvePhase.STATUS`
        //                 to `FirResolvePhase.EXPECT_ACTUAL_MATCHING` (as described in KT-72844), we set the generated function
        //                 resolution phase here as `FirResolvePhase.BODY_RESOLVE`. To avoid the contract violation, we have to
        //                 correctly provide the FIR resolution information in `FirResolvePhase.BODY_RESOLVE` level here.
        val copy = copyFirFunctionWithResolvePhase(original, callableId, SerializationPluginKey, FirResolvePhase.BODY_RESOLVE) {
            status = original.status.copy(modality = Modality.FINAL)
        }

        copy.excludeFromJsExport(session)
        return listOf(copy.symbol)
    }

    private fun generateSerializerFactoryVararg(
        owner: FirClassSymbol<*>,
        callableId: CallableId,
        original: FirSimpleFunction
    ): FirNamedFunctionSymbol =
        createMemberFunction(owner, SerializationPluginKey, callableId.callableName, original.returnTypeRef.coneType) {
            val vpo = original.valueParameters.single()
            valueParameter(vpo.name, vpo.returnTypeRef.coneType, vpo.isCrossinline, vpo.isNoinline, vpo.isVararg, vpo.defaultValue != null)
        }.apply {
            excludeFromJsExport(session)
        }.symbol

    private fun generateSerializerGetterInCompanion(
        owner: FirClassSymbol<*>,
        serializableClassSymbol: FirClassSymbol<*>,
        callableId: CallableId,
        isPublic: Boolean
    ): FirNamedFunctionSymbol {
        val function = createMemberFunction(
            owner,
            SerializationPluginKey,
            callableId.callableName,
            returnTypeProvider = { typeParameters ->
                val parametersAsArguments = typeParameters.map { it.toConeType() }.toTypedArray<ConeTypeProjection>()
                kSerializerId.constructClassLikeType(
                    arrayOf(serializableClassSymbol.constructType(parametersAsArguments)),
                )
            }
        ) {
            serializableClassSymbol.typeParameterSymbols.forEachIndexed { i, typeParameterSymbol ->
                typeParameter(typeParameterSymbol.name)
                valueParameter(
                    Name.identifier("${SerialEntityNames.typeArgPrefix}$i"),
                    { typeParameters ->
                        kSerializerId.constructClassLikeType(arrayOf(typeParameters[i].toConeType()), false)
                    }
                )
            }

            visibility = if (isPublic) Visibilities.Public else Visibilities.Internal
        }

        function.excludeFromJsExport(session)

        return function.symbol
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (!owner.isSerializer) return emptyList()
        if (callableId.callableName != SerialEntityNames.SERIAL_DESC_FIELD_NAME) return emptyList()

        val target = getFromSupertype(callableId, owner) { it.getProperties(callableId.callableName).filterIsInstance<FirPropertySymbol>() }
        val property = createMemberProperty(
            owner,
            SerializationPluginKey,
            callableId.callableName,
            target.resolvedReturnType
        )

        property.excludeFromJsExport(session)

        return listOf(property.symbol)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner

        val result = mutableListOf<FirConstructorSymbol>()
        result += createDefaultPrivateConstructor(owner, SerializationPluginKey).symbol

        if (owner.name == SerialEntityNames.SERIALIZER_CLASS_NAME && owner.typeParameterSymbols.isNotEmpty()) {
            result += createConstructor(owner, SerializationPluginKey) {
                visibility = Visibilities.Public
                owner.typeParameterSymbols.forEachIndexed { i, typeParam ->
                    valueParameter(
                        name = Name.identifier("${SerialEntityNames.typeArgPrefix}$i"),
                        type = kSerializerId.constructClassLikeType(arrayOf(typeParam.toConeType()), false)
                    )
                }
            }.also {
                it.containingClassForStaticMemberAttr = owner.toLookupTag()
            }.symbol
        }
        return result
    }

    private fun generateSerializerImplClass(owner: FirRegularClassSymbol): FirClassLikeSymbol<*> {
        val hasTypeParams = owner.typeParameterSymbols.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerFirClass = createNestedClass(owner, SerialEntityNames.SERIALIZER_CLASS_NAME, SerializationPluginKey, serializerKind) {
            modality = Modality.FINAL

            for (parameter in owner.typeParameterSymbols) {
                typeParameter(parameter.name)
            }
            superType { typeParameters ->
                generatedSerializerId.constructClassLikeType(
                    arrayOf(
                        owner.constructType(
                            typeParameters.map { it.toConeType() }.toTypedArray(),
                        )
                    ),
                )
            }
        }.apply {
            excludeFromJsExport(session)
            markAsDeprecatedHidden(session)
        }
        return serializerFirClass.symbol
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, SerializationPluginKey) {
            if (owner.companionNeedsSerializerFactory(session)) {
                val serializerFactoryClassId = ClassId(SerializationPackages.internalPackageFqName, SERIALIZER_FACTORY_INTERFACE_NAME)
                superType(serializerFactoryClassId.constructClassLikeType(emptyArray(), false))
            }
        }

        return companion.symbol
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(
            FirSerializationPredicates.annotatedWithSerializableOrMeta,
            FirSerializationPredicates.hasMetaAnnotation,
            // registering predicate so that the annotation `KeepGeneratedSerializer` will be resolved on COMPILER_REQUIRED_ANNOTATIONS phase
            FirSerializationPredicates.annotatedWithKeepSerializer
        )
    }

    private val FirClassSymbol<*>.isSerializer: Boolean
        get() = name == SerialEntityNames.SERIALIZER_CLASS_NAME || isExternalSerializer

    private val FirClassSymbol<*>.isExternalSerializer: Boolean
        get() = session.predicateBasedProvider.matches(FirSerializationPredicates.serializerFor, this)

}
