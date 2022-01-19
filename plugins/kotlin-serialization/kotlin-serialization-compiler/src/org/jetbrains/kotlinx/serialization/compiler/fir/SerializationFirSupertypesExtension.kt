/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

class SerializationFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val serializerFor: DeclarationPredicate =
            AnnotatedWith(setOf(SerializationAnnotations.serializerAnnotationFqName)) // @Serializer(for=...)
        private val generatedSerializer: DeclarationPredicate =
            ancestorAnnotated(SerializationAnnotations.serializableAnnotationFqName) // @Serializable X.$serializer
        private val PREDICATE = serializerFor or generatedSerializer
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        session.predicateBasedProvider.matches(serializerFor, declaration)
                || ( // TODO: this part is not working
                session.predicateBasedProvider.matches(generatedSerializer, declaration)
                        && declaration is FirRegularClass
                        && declaration.name == SerialEntityNames.SERIALIZER_CLASS_NAME
                )

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef> {
        val kSerializerClassId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)
        val generatedSerializerClassId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)
        if (resolvedSupertypes.any { it.type.classId == kSerializerClassId || it.type.classId == generatedSerializerClassId }) return emptyList()


        if (session.predicateBasedProvider.matches(serializerFor, classLikeDeclaration)) {
            TODO("Support @Serializer(for=...) supertype generation")
        } else {
            // TODO: Remove this section as it is not working because one should add supertype for generated class where this class is created.
            // @Serializable X.$serializer
            val parent = (classLikeDeclaration.getContainingDeclaration(session) as? FirRegularClass)
                ?: error("Generated serializer $classLikeDeclaration is not contained in any class")

            return listOf(buildResolvedTypeRef {
                // FIXME: document how one gets type ref from FirClass
                // FIXME: is this a correct one or should I go with buildTypeProjectionWithVariance ?
                // It seems that this code generates KSerializer<Box<[declared type param]>> instead of KSerializer<Box<*>>, but it didn't matter for old FE
                type = generatedSerializerClassId.constructClassLikeType(arrayOf(parent.defaultType().toTypeProjection(Variance.INVARIANT)), isNullable = false)
            })
        }
    }
}