/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationPredicates.annotatedWithSerializableOrMeta
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationPredicates.serializerFor
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

class SerializationFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {

    private val isJvmOrMetadata = !session.moduleData.platform.run { isNative() || isJs() || isWasm() }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        session.predicateBasedProvider.matches(serializerFor, declaration) || isSerializableObjectAndNeedsFactory(declaration) || isCompanionAndNeedsFactory(declaration)

    private fun isSerializableObjectAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean = with(session) {
        if (isJvmOrMetadata) return false
        return declaration is FirClass && declaration.classKind.isObject
                && session.predicateBasedProvider.matches(annotatedWithSerializableOrMeta, declaration)
    }

    private fun isCompanionAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean = with(session) {
        if (isJvmOrMetadata) return false
        if (declaration !is FirRegularClass) return false
        if (!declaration.isCompanion) return false
        val parentSymbol = declaration.symbol.getContainingDeclarationSymbol(session) as FirClassSymbol<*>
        return session.predicateBasedProvider.matches(annotatedWithSerializableOrMeta, parentSymbol)
                && parentSymbol.companionNeedsSerializerFactory
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(serializerFor)
    }

    context(TypeResolveServiceContainer)
    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
    ): List<FirResolvedTypeRef> {
        val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)
        val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)

        return when {
            session.predicateBasedProvider.matches(serializerFor, classLikeDeclaration) -> {
                if (resolvedSupertypes.any { it.type.classId == kSerializerClassId || it.type.classId == generatedSerializerClassId }) return emptyList()
                val getClassArgument = classLikeDeclaration.getSerializerFor(session) ?: return emptyList()
                val serializerConeType = resolveConeTypeFromArgument(getClassArgument)

                listOf(
                    buildResolvedTypeRef {
                        type = kSerializerClassId.constructClassLikeType(arrayOf(serializerConeType), isNullable = false)
                    }
                )
            }
            isSerializableObjectAndNeedsFactory(classLikeDeclaration) || isCompanionAndNeedsFactory(classLikeDeclaration) -> {
                val serializerFactoryClassId = ClassId(
                    SerializationPackages.internalPackageFqName,
                    SerialEntityNames.SERIALIZER_FACTORY_INTERFACE_NAME
                )
                if (resolvedSupertypes.any { it.type.classId == serializerFactoryClassId }) return emptyList()
                listOf(serializerFactoryClassId.constructClassLikeType(emptyArray(), false).toFirResolvedTypeRef())
            }
            else -> emptyList()
        }
    }

    // Function helps to resolve class call from annotation argument to `ConeKotlinType`
    context(TypeResolveServiceContainer)
    private fun resolveConeTypeFromArgument(getClassCall: FirGetClassCall): ConeKotlinType {
        val typeToResolve = buildUserTypeFromQualifierParts(isMarkedNullable = false) {
            fun visitQualifiers(expression: FirExpression) {
                if (expression !is FirPropertyAccessExpression) return
                expression.explicitReceiver?.let { visitQualifiers(it) }
                expression.qualifierName?.let { part(it) }
            }
            visitQualifiers(getClassCall.argument)
        }
        return typeResolver.resolveUserType(typeToResolve).type
    }

    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name
}
