/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.isJs
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
        val hasAnnotatedFactory = session.symbolProvider.getTopLevelCallableSymbols(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
        ).isNotEmpty()
        hasFactory && hasAnnotatedFactory
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        with(session) {
            if (classSymbol.shouldHaveGeneratedMethodsInCompanion && !classSymbol.isSerializableObject)
                result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            if (classSymbol.shouldHaveGeneratedSerializer && !classSymbol.isInternallySerializableObject)
                result += SerialEntityNames.SERIALIZER_CLASS_NAME
        }

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
        val function = createMemberFunction(
            owner,
            SerializationPluginKey,
            callableId.callableName,
            returnTypeProvider = { typeParameters ->
                val parametersAsArguments = typeParameters.map { it.toConeType() }.toTypedArray<ConeTypeProjection>()
                kSerializerId.constructClassLikeType(
                    arrayOf(serializableClassSymbol.constructType(parametersAsArguments, false)),
                    isNullable = false
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
        }

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

        return listOf(property.symbol)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner

        val result = mutableListOf<FirConstructorSymbol>()
        result += createDefaultPrivateConstructor(owner, SerializationPluginKey).symbol

        if (owner.name == SerialEntityNames.SERIALIZER_CLASS_NAME && owner.typeParameterSymbols.isNotEmpty()) {
            result += createConstructor(owner, SerializationPluginKey) {
                visibility = Visibilities.Private
                owner.typeParameterSymbols.forEachIndexed { i, typeParam ->
                    valueParameter(
                        name = Name.identifier("${SerialEntityNames.typeArgPrefix}$i"),
                        type = kSerializerId.constructClassLikeType(arrayOf(typeParam.toConeType()), false)
                    )
                }
            }.also {
                it.containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(owner.classId)
            }.symbol
        }
        return result
    }

    private fun generateSerializerImplClass(owner: FirRegularClassSymbol): FirClassLikeSymbol<*> {
        val hasTypeParams = owner.typeParameterSymbols.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerFirClass = createNestedClass(owner, SerialEntityNames.SERIALIZER_CLASS_NAME, SerializationPluginKey, serializerKind) {
            for (parameter in owner.typeParameterSymbols) {
                typeParameter(parameter.name)
            }
            superType { typeParameters ->
                generatedSerializerId.constructClassLikeType(
                    arrayOf(
                        owner.constructType(
                            typeParameters.map { it.toConeType() }.toTypedArray(),
                            isNullable = false
                        )
                    ),
                    isNullable = false
                )
            }
        }
        // TODO: add deprecate hidden
        // serializerFirClass.replaceAnnotations(listOf(Annotations.create(listOf(KSerializerDescriptorResolver.createDeprecatedHiddenAnnotation(thisDescriptor.module)))))

        return serializerFirClass.symbol
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, SerializationPluginKey) {
            if (with(session) { owner.companionNeedsSerializerFactory }) {
                val serializerFactoryClassId = ClassId(SerializationPackages.internalPackageFqName, SERIALIZER_FACTORY_INTERFACE_NAME)
                superType(serializerFactoryClassId.constructClassLikeType(emptyArray(), false))
            }
        }
        return companion.symbol
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
