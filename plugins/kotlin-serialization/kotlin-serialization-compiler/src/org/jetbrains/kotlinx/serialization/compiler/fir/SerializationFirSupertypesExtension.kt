/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationPredicates.serializerFor
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

class SerializationFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        session.predicateBasedProvider.matches(serializerFor, declaration)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(serializerFor)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef> {
        val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)
        val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)
        if (resolvedSupertypes.any { it.type.classId == kSerializerClassId || it.type.classId == generatedSerializerClassId }) return emptyList()


        return if (session.predicateBasedProvider.matches(serializerFor, classLikeDeclaration)) {
            TODO("Support @Serializer(for=...) supertype generation")
        } else emptyList()
    }
}