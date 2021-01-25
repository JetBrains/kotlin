/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getTowerDataContextUnsafe
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeContext
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirAnonymousObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

internal class KtFirScopeProvider(
    analysisSession: KtFirAnalysisSession,
    builder: KtSymbolByFirBuilder,
    private val project: Project,
    firResolveState: FirModuleResolveState,
    override val token: ValidityToken,
) : KtScopeProvider(), ValidityTokenOwner {
    override val analysisSession: KtFirAnalysisSession by weakRef(analysisSession)
    private val builder by weakRef(builder)
    private val firResolveState by weakRef(firResolveState)
    private val firScopeStorage = FirScopeRegistry()

    private val memberScopeCache = IdentityHashMap<KtSymbolWithMembers, KtMemberScope>()
    private val declaredMemberScopeCache = IdentityHashMap<KtSymbolWithMembers, KtDeclaredMemberScope>()
    private val fileScopeCache = IdentityHashMap<KtFileSymbol, KtDeclarationScope<KtSymbolWithDeclarations>>()
    private val packageMemberScopeCache = IdentityHashMap<KtPackageSymbol, KtPackageScope>()

    private inline fun <T> KtSymbolWithMembers.withFirForScope(crossinline body: (FirClass<*>) -> T): T? = when (this) {
        is KtFirClassOrObjectSymbol -> firRef.withFir(FirResolvePhase.SUPER_TYPES, body)
        is KtFirAnonymousObjectSymbol -> firRef.withFir(FirResolvePhase.SUPER_TYPES, body)
        is KtFirEnumEntrySymbol -> firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            val initializer = it.initializer
            check(initializer is FirAnonymousObject)
            body(initializer)
        }
        else -> error { "Unknown KtSymbolWithDeclarations implementation ${this::class.qualifiedName}" }
    }

    override fun getMemberScope(classSymbol: KtSymbolWithMembers): KtMemberScope = withValidityAssertion {
        memberScopeCache.getOrPut(classSymbol) {

            val firScope = classSymbol.withFirForScope { fir ->
                val firSession = fir.session
                fir.unsubstitutedScope(
                    firSession,
                    ScopeSession(),
                    withForcedTypeCalculator = false
                )
            } ?: return@getOrPut KtFirEmptyMemberScope(classSymbol)

            firScopeStorage.register(firScope)
            KtFirMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtDeclaredMemberScope = withValidityAssertion {
        declaredMemberScopeCache.getOrPut(classSymbol) {
            val firScope = classSymbol.withFirForScope {
                declaredMemberScope(it)
            } ?: return@getOrPut KtFirEmptyMemberScope(classSymbol)

            firScopeStorage.register(firScope)

            KtFirDeclaredMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getFileScope(fileSymbol: KtFileSymbol): KtDeclarationScope<KtSymbolWithDeclarations> = withValidityAssertion {
        fileScopeCache.getOrPut(fileSymbol) {
            check(fileSymbol is KtFirFileSymbol) { "KtFirScopeProvider can only work with KtFirFileSymbol, but ${fileSymbol::class} was provided" }
            KtFirFileScope(fileSymbol, token, builder)
        }
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope = withValidityAssertion {
        packageMemberScopeCache.getOrPut(packageSymbol) {
            val firPackageScope =
                FirPackageMemberScope(
                    packageSymbol.fqName,
                    firResolveState.rootModuleSession/*TODO use correct session here*/
                ).also(firScopeStorage::register)
            KtFirPackageScope(firPackageScope, project, builder, token)
        }
    }

    override fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope = withValidityAssertion {
        KtFirCompositeScope(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        val firSession = firResolveState.rootModuleSession
        val firTypeScope = type.coneType.scope(
            firSession,
            ScopeSession(),
            FakeOverrideTypeCalculator.Forced
        ) ?: return null
        return getCompositeScope(
            listOf(
                convertToKtScope(firTypeScope),
                firTypeScope.getSyntheticPropertiesScope(firSession)
            )
        )
    }

    private fun FirTypeScope.getSyntheticPropertiesScope(firSession: FirSession): KtScope =
        convertToKtScope(FirSyntheticPropertiesScope(firSession, this))

    override fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext = withValidityAssertion {
        val towerDataContext = analysisSession.firResolveState.getTowerDataContextUnsafe(positionInFakeFile)

        val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }.distinct()
        val implicitReceiversTypes = implicitReceivers.map { builder.buildKtType(it.type) }

        val implicitReceiverScopes = implicitReceivers.mapNotNull { it.implicitScope }
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }.distinct()
        val firLocalScopes = towerDataContext.localScopes

        @OptIn(ExperimentalStdlibApi::class)
        val allKtScopes = buildList {
            implicitReceiverScopes.mapTo(this, ::convertToKtScope)
            nonLocalScopes.mapTo(this, ::convertToKtScope)
            firLocalScopes.mapTo(this, ::convertToKtScope)
        }

        KtScopeContext(
            getCompositeScope(allKtScopes.asReversed()),
            implicitReceiversTypes.asReversed()
        )
    }

    private fun convertToKtScope(firScope: FirScope): KtScope {
        firScopeStorage.register(firScope)
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder, token)
            is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, builder, project, token)
            is FirPackageMemberScope -> KtFirPackageScope(firScope, project, builder, token)
            is FirContainingNamesAwareScope -> KtFirDelegatingScopeImpl(firScope, builder, token)
            is FirMemberTypeParameterScope -> KtFirDelegatingScopeImpl(firScope, builder, token)
            else -> TODO(firScope::class.toString())
        }
    }
}

private class KtFirDelegatingScopeImpl<S>(
    firScope: S, builder: KtSymbolByFirBuilder,
    token: ValidityToken
) : KtFirDelegatingScope<S>(builder, token), ValidityTokenOwner where S : FirContainingNamesAwareScope, S : FirScope {
    override val firScope: S by weakRef(firScope)
}

/**
 * Stores strong references to all instances of [FirScope] used
 * Needed as the only entity which may have a strong references to FIR internals is [KtFirAnalysisSession] & [KtAnalysisSessionComponent]
 * Entities which needs storing [FirScope] instances will store them as weak references via [org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef]
 */
internal class FirScopeRegistry {
    private val scopes = mutableListOf<FirScope>()

    fun register(scope: FirScope) {
        scopes += scope
    }
}
