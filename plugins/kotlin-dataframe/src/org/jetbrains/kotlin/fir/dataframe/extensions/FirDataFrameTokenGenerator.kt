package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

class FirDataFrameTokenGenerator(session: FirSession, val tokens: Set<ClassId>, val tokenState: Map<ClassId, SchemaContext>) : FirDeclarationGenerationExtension(session) {
    object Key : GeneratedDeclarationKey()

    override fun getTopLevelClassIds(): Set<ClassId> {
        return tokens
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId !in tokens) return null
        val klass = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(FirDataFrameReceiverInjector.DataFramePluginKey)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Internal, Modality.FINAL, EffectiveVisibility.Internal)
            classKind = ClassKind.INTERFACE
            scopeProvider = FirKotlinScopeProvider()
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
        }
        return klass.symbol
    }
}
