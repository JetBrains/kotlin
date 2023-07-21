/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.classSerializer
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.currentFile
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.overriddenSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.*


class ContextualSerializersProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val contextualKClassListCache: FirCache<FirFile, Set<ConeKotlinType>, Nothing?> =
        session.firCachesFactory.createCache { file ->
            buildSet {
                addAll(getKClassListFromFileAnnotation(file, SerializationAnnotations.contextualClassId))
                addAll(getKClassListFromFileAnnotation(file, SerializationAnnotations.contextualOnFileClassId))
            }
        }

    fun getContextualKClassListForFile(file: FirFile): Set<ConeKotlinType> {
        return contextualKClassListCache.getValue(file)
    }

    private val additionalSerializersInScopeCache: FirCache<FirFile, Map<Pair<FirClassSymbol<*>, Boolean>, FirClassSymbol<*>>, Nothing?> =
        session.firCachesFactory.createCache { file ->
            getKClassListFromFileAnnotation(file, SerializationAnnotations.additionalSerializersClassId).associateBy(
                keySelector = {
                    val serializerType = it.serializerForType(session)
                    val symbol = serializerType?.toRegularClassSymbol(session)
                        ?: throw AssertionError("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                    symbol to serializerType.isMarkedNullable
                },
                valueTransform = { it.toRegularClassSymbol(session)!! }
            )
        }

    fun getAdditionalSerializersInScopeForFile(file: FirFile): Map<Pair<FirClassSymbol<*>, Boolean>, FirClassSymbol<*>> {
        return additionalSerializersInScopeCache.getValue(file)
    }

    private fun getKClassListFromFileAnnotation(file: FirFile, annotationClassId: ClassId): List<ConeKotlinType> {
        val annotation = file.symbol.resolvedAnnotationsWithArguments.getAnnotationByClassId(
            annotationClassId, session
        ) ?: return emptyList()
        val arguments = when (val argument = annotation.argumentMapping.mapping.values.firstOrNull()) {
            is FirArrayLiteral -> argument.arguments
            is FirVarargArgumentsExpression -> argument.arguments
            else -> return emptyList()
        }
        return arguments.mapNotNull { (it as? FirGetClassCall)?.getTargetType() }
    }
}

val FirSession.contextualSerializersProvider: ContextualSerializersProvider by FirSession.sessionComponentAccessor()

context(CheckerContext)
fun findTypeSerializerOrContextUnchecked(type: ConeKotlinType): FirClassSymbol<*>? {
    if (type.isTypeParameter) return null
    val annotations = type.fullyExpandedType(session).customAnnotations
    annotations.getSerializableWith(session)?.let { return it.toRegularClassSymbol(session) }
    val classSymbol = type.toRegularClassSymbol(session) ?: return null
    val currentFile = currentFile
    val provider = session.contextualSerializersProvider
    provider.getAdditionalSerializersInScopeForFile(currentFile)[classSymbol to type.isMarkedNullable]?.let { return it }
    if (type.isMarkedNullable) {
        return findTypeSerializerOrContextUnchecked(type.withNullability(ConeNullability.NOT_NULL, session.typeContext))
    }
    if (type in provider.getContextualKClassListForFile(currentFile)) {
        return session.dependencySerializationInfoProvider.getClassFromSerializationPackage(SpecialBuiltins.Names.contextSerializer)
    }
    return analyzeSpecialSerializers(session, annotations) ?: findTypeSerializer(type)
}

/**
 * Returns class descriptor for ContextSerializer or PolymorphicSerializer
 * if [annotations] contains @Contextual or @Polymorphic annotation
 */
fun analyzeSpecialSerializers(session: FirSession, annotations: List<FirAnnotation>): FirClassSymbol<*>? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualClassId, session) ||
            annotations.hasAnnotation(SerializationAnnotations.contextualOnPropertyClassId, session) -> {
        session.dependencySerializationInfoProvider.getClassFromSerializationPackage(SpecialBuiltins.Names.contextSerializer)
    }
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicClassId, session) -> {
        session.dependencySerializationInfoProvider.getClassFromSerializationPackage(SpecialBuiltins.Names.polymorphicSerializer)
    }

    else -> null
}

context(CheckerContext)
fun findTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val userOverride = type.overriddenSerializer
    if (userOverride != null) return userOverride.toRegularClassSymbol(session)
    if (type.isTypeParameter) return null
    val serializationProvider = session.dependencySerializationInfoProvider
    if (type.isArrayType) {
        return serializationProvider.getClassFromInternalSerializationPackage(SpecialBuiltins.Names.referenceArraySerializer)
    }
    if (with(session) { type.isGeneratedSerializableObject }) {
        return serializationProvider.getClassFromInternalSerializationPackage(SpecialBuiltins.Names.objectSerializer)
    }
    // see if there is a standard serializer
    val standardSerializer = with(session) { findStandardKotlinTypeSerializer(type) ?: findEnumTypeSerializer(type) }
    if (standardSerializer != null) return standardSerializer
    val symbol = type.toRegularClassSymbol(session) ?: return null
    if (with(session) { symbol.isSealedSerializableInterface }) {
        return serializationProvider.getClassFromSerializationPackage(SpecialBuiltins.Names.polymorphicSerializer)
    }
    return symbol.classSerializer // check for serializer defined on the type
}

context(FirSession)
fun findStandardKotlinTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val name = when {
        type.isBoolean -> PrimitiveBuiltins.booleanSerializer
        type.isByte -> PrimitiveBuiltins.byteSerializer
        type.isShort -> PrimitiveBuiltins.shortSerializer
        type.isInt -> PrimitiveBuiltins.intSerializer
        type.isLong -> PrimitiveBuiltins.longSerializer
        type.isFloat -> PrimitiveBuiltins.floatSerializer
        type.isDouble -> PrimitiveBuiltins.doubleSerializer
        type.isChar -> PrimitiveBuiltins.charSerializer
        else -> findStandardKotlinTypeSerializerName(type.classId?.asFqNameString())
    }?.let(Name::identifier) ?: return null
    val symbolProvider = symbolProvider
    return symbolProvider.getClassLikeSymbolByClassId(ClassId(SerializationPackages.internalPackageFqName, name)) as? FirClassSymbol<*>
        ?: symbolProvider.getClassLikeSymbolByClassId(ClassId(SerializationPackages.packageFqName, name)) as? FirClassSymbol<*>
}

context(FirSession)
fun findEnumTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val symbol = type.toRegularClassSymbol(this@FirSession) ?: return null
    return runIf(symbol.isEnumClass && !symbol.isEnumWithLegacyGeneratedSerializer) {
        symbolProvider.getClassLikeSymbolByClassId(SerializersClassIds.enumSerializerId) as? FirClassSymbol<*>
    }
}
