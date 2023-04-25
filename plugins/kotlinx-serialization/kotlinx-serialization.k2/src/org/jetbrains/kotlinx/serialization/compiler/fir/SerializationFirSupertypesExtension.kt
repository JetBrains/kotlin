/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationPredicates.serializerFor
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

class SerializationFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        session.predicateBasedProvider.matches(serializerFor, declaration)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(serializerFor)
    }

    context(TypeResolveServiceContainer)
    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef> {
        val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)
        val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)
        if (resolvedSupertypes.any { it.type.classId == kSerializerClassId || it.type.classId == generatedSerializerClassId }) return emptyList()

        return if (session.predicateBasedProvider.matches(serializerFor, classLikeDeclaration)) {
            val getClassArgument = classLikeDeclaration.getSerializerFor(session) ?: return emptyList()
            val serializerConeType = resolveConeTypeFromArgument(getClassArgument)

            listOf(
                buildResolvedTypeRef {
                    type = kSerializerClassId.constructClassLikeType(arrayOf(serializerConeType), isNullable = false)
                }
            )
        } else emptyList()
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
