/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * Adds MyInterface supertype for all classes annotated with @MyInterfaceSupertype or meta-annotated with MetaSupertype
 */
class SomeAdditionalSupertypeGenerator(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val myInterfaceClassId = ClassId(FqName("foo"), Name.identifier("MyInterface"))
        private val PREDICATE = DeclarationPredicate.create {
            annotated("MyInterfaceSupertype".fqn()) or metaAnnotated("MetaSupertype".fqn(), includeItself = false)
        }
    }

    context(TypeResolveServiceContainer)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef> {
        if (classLikeDeclaration !is FirRegularClass) return emptyList()
        when (classLikeDeclaration.classKind) {
            ClassKind.CLASS,
            ClassKind.INTERFACE,
            ClassKind.OBJECT-> {}

            ClassKind.ENUM_CLASS,
            ClassKind.ENUM_ENTRY,
            ClassKind.ANNOTATION_CLASS -> return emptyList()
        }
        if (resolvedSupertypes.any { it.type.classId == myInterfaceClassId }) return emptyList()
        return listOf(
            buildResolvedTypeRef {
                type = myInterfaceClassId.constructClassLikeType(emptyArray(), isNullable = false)
            }
        )
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
