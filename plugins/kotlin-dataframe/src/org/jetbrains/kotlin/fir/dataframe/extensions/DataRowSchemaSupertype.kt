package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

class DataRowSchemaSupertype(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create {
            annotated(AnnotationFqn(DataSchema::class.java.name))
        }
        private val dataRowSchema = ClassId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("DataRowSchema"))
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return declaration is FirRegularClass
            && declaration.classKind == ClassKind.CLASS
            && session.predicateBasedProvider.matches(PREDICATE, declaration)
    }


    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<FirResolvedTypeRef> {
        if (resolvedSupertypes.any { it.toClassLikeSymbol(session)?.classId == dataRowSchema }) return emptyList()
        return listOf(
            buildResolvedTypeRef {
                type = dataRowSchema.constructClassLikeType(emptyArray())
            }
        )
    }
}
