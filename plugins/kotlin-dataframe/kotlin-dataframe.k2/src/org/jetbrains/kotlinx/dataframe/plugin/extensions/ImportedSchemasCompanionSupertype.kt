package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toPluginKey
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.PluginTypeResolver
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaCompanionKey
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ImportedSchemasCompanionSupertype(
    session: FirSession,
    val predicate: LookupPredicate,
) : FirSupertypeGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return declaration is FirRegularClass &&
                declaration.isCompanion &&
                declaration.origin.toPluginKey<ImportedSchemaCompanionKey>() != null &&
                session.predicateBasedProvider.matches(predicate, declaration)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: PluginTypeResolver,
    ): List<ConeKotlinType> {
        return listOf(Names.DATAFRAME_PROVIDER.constructClassLikeType(arrayOf(classLikeDeclaration.defaultType())))
    }
}
