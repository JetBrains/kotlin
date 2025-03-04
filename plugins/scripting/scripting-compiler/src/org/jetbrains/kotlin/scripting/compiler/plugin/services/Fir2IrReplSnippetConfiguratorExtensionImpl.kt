/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.backend.jvm.originalSnippetValueSymbol
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrVisitor
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.script.experimental.api.ReplScriptingHostConfigurationKeys
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

/**
 * FQName of the state object (Map<String, Any?) if provided externally
 *
 * if is not provided - generated automatically along with the first snippet class
 */
val ReplScriptingHostConfigurationKeys.replStateObjectFqName by PropertiesCollection.key<String>()

private val replStateDefaultName = Name.identifier("ReplState")

class Fir2IrReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    private val hostConfiguration: ScriptingHostConfiguration,
) : Fir2IrReplSnippetConfiguratorExtension(session) {

    @OptIn(SymbolInternals::class)
    override fun Fir2IrComponents.prepareSnippet(fir2IrVisitor: Fir2IrVisitor, firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet) {
        val propertiesFromState = hashMapOf<FirPropertySymbol, FirReplSnippetSymbol>()
        val functionsFromState = hashMapOf<FirNamedFunctionSymbol, FirReplSnippetSymbol>()
        val classesFromState = hashMapOf<FirRegularClassSymbol, FirReplSnippetSymbol>()
        val usedOtherSnippets = HashSet<FirReplSnippetSymbol>()

        CollectAccessToOtherState(
            session,
            propertiesFromState,
            functionsFromState,
            classesFromState,
            usedOtherSnippets
        ).visitReplSnippet(firReplSnippet)

        usedOtherSnippets.remove(firReplSnippet.symbol)
        usedOtherSnippets.forEach {
            val packageFragment = declarationStorage.getIrExternalPackageFragment(it.packageFqName(), it.moduleData)
            classifierStorage.createAndCacheEarlierSnippetClass(it, packageFragment)
        }

        // Classes should be created first to populate the cache before possible references
        classesFromState.forEach { (classSymbol, snippetSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                createClassFromOtherSnippet(classSymbol, originalSnippet, irSnippet)
            }
        }

        propertiesFromState.forEach { (propertySymbol, snippetSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                declarationStorage.createAndCacheIrVariable(
                    propertySymbol.fir, irSnippet, IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                ).also { varFromOtherSnippet ->
                    irSnippet.variablesFromOtherSnippets.add(varFromOtherSnippet)
                    val field = originalSnippet.addField {
                        name = varFromOtherSnippet.name
                        type = varFromOtherSnippet.type
                        visibility = DescriptorVisibilities.PUBLIC
                        origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                    }
                    varFromOtherSnippet.originalSnippetValueSymbol = field.symbol
                }
            }
        }

        functionsFromState.forEach { (functionSymbol, snippetSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                val actualParent = getOrBuildActualParent(functionSymbol, originalSnippet, irSnippet)
                declarationStorage.createAndCacheIrFunction(
                    functionSymbol.fir,
                    actualParent,
                    predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                    fakeOverrideOwnerLookupTag = null,
                    allowLazyDeclarationsCreation = true
                ).run {
                    parent = actualParent
                    visibility = DescriptorVisibilities.PUBLIC
                    irSnippet.declarationsFromOtherSnippets.add(this)
                }
            }
        }

        val stateObject =
            getStateObject(
                irSnippet,
                fir2IrVisitor,
                createIfNotFound = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName] == null &&
                        hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]!!.isFirstSnippet(firReplSnippet.symbol)
            )

        irSnippet.stateObject = stateObject.symbol
    }

    private fun Fir2IrComponents.getOrBuildActualParent(
        symbol: FirBasedSymbol<*>, parentClassOrSnippet: IrClass, irSnippet: IrReplSnippet
    ): IrClass =
        symbol.getContainingClassSymbol()?.let {
            if (it is FirRegularClassSymbol)
                createClassFromOtherSnippet(it, parentClassOrSnippet, irSnippet)
            else null
        } ?: parentClassOrSnippet

    @OptIn(SymbolInternals::class, DelicateDeclarationStorageApi::class)
    private fun Fir2IrComponents.createClassFromOtherSnippet(
        classSymbol: FirRegularClassSymbol,
        parentClassOrSnippet: IrClass,
        irSnippet: IrReplSnippet
    ): IrClass {
        val actualParent = getOrBuildActualParent(classSymbol, parentClassOrSnippet, irSnippet)
        return classifierStorage.getFir2IrLazyClass(classSymbol.fir).apply {
            parent = actualParent
            origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
            irSnippet.declarationsFromOtherSnippets.add(this)
        }
    }

    @OptIn(SymbolInternals::class, LookupTagInternals::class, DirectDeclarationsAccess::class)
    private fun Fir2IrComponents.getStateObject(
        irSnippet: IrReplSnippet,
        fir2IrVisitor: Fir2IrVisitor,
        createIfNotFound: Boolean,
    ): IrClass {
        fun fqn2cid(s: String): ClassId {
            val fqn = FqName(s)
            return ClassId(fqn.parent(), fqn.shortName())
        }

        val classId = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName]?.let(::fqn2cid)
            ?: ClassId(irSnippet.getPackageFragment().packageFqName, replStateDefaultName)

        val firReplStateFromDependencies =
            (session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)?.fir

        val firReplStateObject = firReplStateFromDependencies ?: run {
            val hashMapClassSymbol =
                session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(
                    fqn2cid("kotlin.collections.HashMap")
                )?.fullyExpandedClass(session) ?: error("HashMap class not found")
            val firReplStateSymbol = FirRegularClassSymbol(classId)
            val constructor = buildPrimaryConstructor {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.FromOtherReplSnippet
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                symbol = FirConstructorSymbol(classId)
                returnTypeRef = FirImplicitTypeRefImplWithoutSource
                dispatchReceiverType = firReplStateSymbol.constructType()
            }
            buildRegularClass {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.FromOtherReplSnippet
                this.name = classId.shortClassName
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                classKind = ClassKind.OBJECT
                symbol = firReplStateSymbol
                superTypeRefs += hashMapClassSymbol.defaultType().toFirResolvedTypeRef(null)
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                scopeProvider = session.kotlinScopeProvider
                declarations += constructor
            }.also {
                (it.symbol.toLookupTag() as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(session, it.symbol)
                constructor.replaceReturnTypeRef(it.defaultType().toFirResolvedTypeRef())
                val delegatingConstructorCall = buildDelegatedConstructorCall {
                    constructedTypeRef = it.superTypeRefs.singleOrNull() ?: error("No single super type for repl state found")
                    val superConstructorSymbol = hashMapClassSymbol.declaredMemberScope(session, memberRequiredPhase = null)
                        .getDeclaredConstructors()
                        .firstOrNull { it.valueParameterSymbols.isEmpty() }
                        ?: error("No arguments constructor for HashMap not found")
                    calleeReference = buildResolvedNamedReference {
                        name = superConstructorSymbol.name
                        resolvedSymbol = superConstructorSymbol
                    }
                    argumentList = FirEmptyArgumentList
                    isThis = false
                }
                constructor.replaceDelegatedConstructor(delegatingConstructorCall)
            }
        }

        return if (firReplStateFromDependencies == null && createIfNotFound) {
            classifierStorage.createAndCacheIrClass(firReplStateObject, irSnippet.parent).also { irReplStateObject ->
                classifiersGenerator.processClassHeader(firReplStateObject, irReplStateObject)
                declarationStorage.createAndCacheIrConstructor(
                    firReplStateObject.declarations.filterIsInstance<FirPrimaryConstructor>().first(),
                    { irReplStateObject }, isLocal = false
                )
                firReplStateObject.accept(fir2IrVisitor, null)
                Unit
            }
        } else {
            val irReplStateParent =
                declarationStorage.getIrExternalPackageFragment(firReplStateObject.symbol.classId.packageFqName, session.moduleData)
            lazyDeclarationsGenerator.createIrLazyClass(firReplStateObject, irReplStateParent, IrClassSymbolImpl())
        }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> Fir2IrReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

private class CollectAccessToOtherState(
    val session: FirSession,
    val properties: MutableMap<FirPropertySymbol, FirReplSnippetSymbol>,
    val functions: MutableMap<FirNamedFunctionSymbol, FirReplSnippetSymbol>,
    val classes: MutableMap<FirRegularClassSymbol, FirReplSnippetSymbol>,
    val snippets: MutableSet<FirReplSnippetSymbol>,
) : FirDefaultVisitorVoid() {

    private fun storeAccessedSymbol(symbol: FirBasedSymbol<FirDeclaration>) {

        @OptIn(SymbolInternals::class)
        fun FirBasedSymbol<FirDeclaration>.getOriginalSnippetSymbol(): FirReplSnippetSymbol? =
            fir.originalReplSnippetSymbol ?: fir.getContainingClassSymbol()?.getOriginalSnippetSymbol()

        val originalSnippet = symbol.getOriginalSnippetSymbol() ?: return
        snippets.add(originalSnippet)
        when (symbol) {
            is FirPropertySymbol -> properties[symbol] = originalSnippet
            is FirNamedFunctionSymbol -> functions[symbol] = originalSnippet
            is FirRegularClassSymbol -> classes[symbol] = originalSnippet
            else -> {}
        }
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    override fun visitElement(element: FirElement) {
        (element as? FirExpression)?.coneTypeOrNull?.toClassSymbol(session)?.let { storeAccessedSymbol(it) }

        element.acceptChildren(this)
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val resolvedSymbol = resolvedNamedReference.resolvedSymbol
        val symbol = when (resolvedSymbol) {
            is FirConstructorSymbol -> (resolvedSymbol.fir.returnTypeRef as? FirResolvedTypeRef)?.coneType?.toSymbol(session)
            is FirCallableSymbol<*> -> {
                resolvedSymbol.resolvedReturnTypeRef.accept(this)
                resolvedSymbol
            }
            else -> null
        } ?: resolvedSymbol

        storeAccessedSymbol(symbol)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val classSymbol = resolvedTypeRef.coneType.toClassSymbol(session) ?: return
        storeAccessedSymbol(classSymbol)
    }
}
