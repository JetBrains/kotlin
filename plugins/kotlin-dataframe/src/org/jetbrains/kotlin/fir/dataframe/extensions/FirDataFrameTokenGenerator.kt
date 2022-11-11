package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

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

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return tokenState[classSymbol.classId]?.properties?.mapTo(mutableSetOf()) { Name.identifier(it.name) } ?: emptySet()
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val properties = callableId.classId?.let { tokenState[it]?.properties } ?: return emptyList()
        val property = properties.find { it.name == callableId.callableName.identifier } ?: return emptyList()
        val firPropertySymbol = FirPropertySymbol(callableId)
        val resolvedTypeRef = buildResolvedTypeRef {
            type = property.dataRowReturnType
        }
        buildProperty {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(Key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Internal,
                Modality.ABSTRACT,
                EffectiveVisibility.Internal
            )
            this.returnTypeRef = resolvedTypeRef
            val classId = callableId.classId
            if (classId != null) {
                dispatchReceiverType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(classId),
                    emptyArray(),
                    false
                )
            }
            val receiverType = dispatchReceiverType
            val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
            getter = buildPropertyAccessor {
                moduleData = session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Plugin(Key)
                this.returnTypeRef = resolvedTypeRef
                dispatchReceiverType = receiverType
                symbol = firPropertyAccessorSymbol
                propertySymbol = firPropertySymbol
                isGetter = true
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Internal,
                    Modality.ABSTRACT,
                    EffectiveVisibility.Internal
                )
            }.also { firPropertyAccessorSymbol.bind(it) }
            name = callableId.callableName
            symbol = firPropertySymbol
            isVar = false
            isLocal = false
        }.also { firPropertySymbol.bind(it) }
        return listOf(firPropertySymbol)
    }
}
