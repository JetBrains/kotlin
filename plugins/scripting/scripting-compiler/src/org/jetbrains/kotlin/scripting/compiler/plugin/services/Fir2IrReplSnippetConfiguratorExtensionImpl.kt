/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.buildReplSidecarFromFir
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.replSidecarMetadataAttr
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

    // This configurator is created once per FirSession/compile call (see getFactory below) and
    // reused for every snippet of that same call -- so this field naturally caches the "ReplState"
    // IR class across every same-session ("same-batch") snippet's own prepareSnippet() call. This is
    // what lets a batch's second and later snippets reuse the exact class the batch's first snippet
    // created, instead of (incorrectly) trying to resolve it from the classpath, where it does not
    // exist yet -- it is compiled together with, not before, its sibling snippets in the same batch.
    // See getStateObject's KDoc for the full mechanism.
    private var cachedStateObjectIrClass: IrClass? = null

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
        classesFromState.forEach { [classSymbol, snippetSymbol] ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                createClassFromOtherSnippet(classSymbol, originalSnippet, irSnippet)
            }
        }

        propertiesFromState.forEach { [propertySymbol, snippetSymbol] ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                val actualParent = getOrBuildActualParent(propertySymbol, originalSnippet, irSnippet)

                fun IrSimpleFunction.updateAccessor() {
                    origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                    parent = actualParent
                    irSnippet.declarationsFromOtherSnippets.add(this)
                }

                irSnippet.declarationsFromOtherSnippets.add(
                    declarationStorage.createAndCacheIrProperty(
                        propertySymbol.fir,
                        originalSnippet,
                        IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                        allowLazyDeclarationsCreation = true
                    ).apply {
                        parent = actualParent
                        getter?.updateAccessor()
                        setter?.updateAccessor()
                    }
                )
            }
        }

        functionsFromState.forEach { [functionSymbol, snippetSymbol] ->
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

        val stateObject = getStateObject(irSnippet, fir2IrVisitor)

        irSnippet.stateObject = stateObject.symbol

        stashReplSidecarMetadataIfStateless(firReplSnippet, irSnippet)
    }

    /**
     * On the **stateless** REPL compile path, assemble the frontend-derivable
     * `SnippetArtifactSidecar` (the `isReplSnippetDeclaration` member refs with their visibilities
     * plus the file-level imports) and stash its protobuf-wire bytes on [irSnippet] so that
     * `ReplSnippetsToClassesLowering.finalizeReplSnippetClass` can embed them into the wrapper
     * class's `.kotlin_metadata` (via the generic `ProtoBuf.CompilerPluginData` channel).
     *
     * This is a no-op on the stateful/golden path — detected by the host configuration carrying a
     * [ClasspathBackedFirReplHistoryProvider] (the only provider that reconstructs prior snippets
     * purely from a compiled class, so it needs the embedded sidecar) — so stateful snippet
     * `.kotlin_metadata` is left bit-for-bit unchanged.
     *
     * For that provider the embedded sidecar is the sole carrier of the reconstruction payload;
     * everything else a consumer needs (class id, snippet name) is either recoverable from the
     * wrapper class itself or already known out-of-band via its `ClassId`.
     */
    private fun stashReplSidecarMetadataIfStateless(firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet) {
        val historyProvider = hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]
        if (historyProvider !is ClasspathBackedFirReplHistoryProvider) return

        val sidecar = buildReplSidecarFromFir(firSnippet = firReplSnippet, session = session)
        irSnippet.replSidecarMetadataAttr = SnippetArtifactSidecarProtoCodec.encode(sidecar)
    }

    private fun Fir2IrComponents.getOrBuildActualParent(
        symbol: FirBasedSymbol<*>, parentClassOrSnippet: IrClass, irSnippet: IrReplSnippet
    ): IrClass =
        symbol.getContainingClassSymbol()?.let {
            if (it is FirRegularClassSymbol &&
                it.origin != FirDeclarationOrigin.Synthetic.ReplContainerClass &&
                !it.isReconstructedSnippetContainerFor(symbol)
            )
                createClassFromOtherSnippet(it, parentClassOrSnippet, irSnippet)
            else null
        } ?: parentClassOrSnippet

    /**
     * Recognises a **stateless**-reconstructed REPL snippet wrapper as a snippet container.
     *
     * In the stateful path the snippet wrapper carries
     * [FirDeclarationOrigin.Synthetic.ReplContainerClass], so the `origin` check in
     * [getOrBuildActualParent] already treats a top-level snippet declaration's container as the
     * snippet and parents the reconstructed declaration directly on the earlier-snippet IR class.
     *
     * In the stateless path the wrapper is **deserialized** from a prior snippet's
     * `.kotlin_metadata`, so its origin is [FirDeclarationOrigin.Library] — indistinguishable by
     * `origin` from a regular user class declared *inside* a prior snippet (e.g. `class Foo`).
     * Treating the wrapper like such a user class makes `createClassFromOtherSnippet` re-nest the
     * declaration under a freshly-built `Wrapper$Wrapper` class that is never emitted, surfacing at
     * eval time as `NoClassDefFoundError: <Wrapper>$<Wrapper>`.
     *
     * The discriminator is identity: [ClasspathBackedFirReplHistoryProvider] tags every reconstructed
     * top-level snippet declaration with [originalReplSnippetSymbol], whose `snippetClass` *is* the
     * wrapper. So [accessedSymbol] is a direct member of the wrapper iff its
     * `originalReplSnippetSymbol`'s snippet-class symbol equals `this`. A member of a user class
     * declared inside a snippet (`Foo.bar`) has `Foo` as its container, which never equals the
     * wrapper — so such accesses still nest correctly.
     */
    @OptIn(SymbolInternals::class)
    private fun FirRegularClassSymbol.isReconstructedSnippetContainerFor(accessedSymbol: FirBasedSymbol<*>): Boolean {
        val ownerSnippetClassSymbol = accessedSymbol.fir.originalReplSnippetSymbol?.fir?.snippetClass?.symbol
        return ownerSnippetClassSymbol == this
    }

    @OptIn(SymbolInternals::class, DelicateDeclarationStorageApi::class)
    private fun Fir2IrComponents.createClassFromOtherSnippet(
        classSymbol: FirRegularClassSymbol,
        parentClassOrSnippet: IrClass,
        irSnippet: IrReplSnippet
    ): IrClass {
        val actualParent = getOrBuildActualParent(classSymbol, parentClassOrSnippet, irSnippet)
        return classifierStorage.getFir2IrLazyClass(classSymbol.fir).apply {
            // this may be called on the directly found usages as well as on the used declarations parents, so deduplication is needed
            // TODO: consider rearranging the code to avoid potentially ineffective check
            if (!irSnippet.declarationsFromOtherSnippets.contains(this)) {
                parent = actualParent
                origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                irSnippet.declarationsFromOtherSnippets.add(this)
            }
        }
    }

    /**
     * Returns the shared "ReplState" object every snippet's `$$eval` body stores/reads its
     * REPL-shared mutable state under -- created once (by the very first snippet ever compiled)
     * and thereafter always just *referenced*, never recreated, by every later snippet.
     *
     * "Later" spans two, quite different, kinds of prior snippet:
     *  * **A genuinely earlier compile call** (e.g. an earlier daemon `compile()` invocation, in its
     *    own now-finished session): the object is a real, already-compiled class, found via
     *    [org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider] -- it is on this
     *    call's classpath -- and wrapped as a lazy/external reference.
     *  * **A same-batch sibling compiled together with this snippet, earlier in this very session**
     *    (e.g. a synthetic bindings-exposing snippet, immediately followed by the main snippet that
     *    reads/writes that state): the object has **no compiled class at all yet** (codegen for the
     *    whole module only happens once, after every file's body is resolved), so it can never be
     *    found via [dependenciesSymbolProvider] -- but since this configurator instance is itself
     *    created once per session and reused for every one of that session's snippets (see
     *    [getFactory]), [cachedStateObjectIrClass] (set the first time this class is actually
     *    created, below) already holds the exact same [IrClass] a same-batch sibling created just
     *    moments ago, and is reused directly, with no reconstruction of any kind needed.
     *
     * Only when neither the cache nor the classpath has it -- meaning this actually *is* the very
     * first snippet ever compiled for the underlying `ScriptCompilationConfiguration.repl` state --
     * is the class (freshly built in-memory below) really created and scheduled for codegen, and
     * cached for this session's own later same-batch snippets to reuse in turn.
     */
    @OptIn(SymbolInternals::class, LookupTagInternals::class, DirectDeclarationsAccess::class)
    private fun Fir2IrComponents.getStateObject(
        irSnippet: IrReplSnippet,
        fir2IrVisitor: Fir2IrVisitor,
    ): IrClass {
        cachedStateObjectIrClass?.let { return it }

        fun fqn2cid(s: String): ClassId {
            val fqn = FqName(s)
            return ClassId(fqn.parent(), fqn.shortName())
        }

        val explicitClassId = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName]?.let(::fqn2cid)
        val classId = explicitClassId ?: ClassId(irSnippet.getPackageFragment().packageFqName, replStateDefaultName)

        val firReplStateFromDependencies =
            (session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)?.fir

        val firReplStateObject = firReplStateFromDependencies ?: run {
            val hashMapClassSymbol =
                session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(
                    fqn2cid("kotlin.collections.HashMap")
                )?.fullyExpandedClass() ?: error("HashMap class not found")
            val firReplStateSymbol = FirRegularClassSymbol(classId)
            val constructor = buildPrimaryConstructor {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.FromOtherReplSnippet
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                isLocal = false
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
                val nullableAnyType = session.builtinTypes.nullableAnyType.coneType
                superTypeRefs += hashMapClassSymbol.constructType(
                    Array(hashMapClassSymbol.typeParameterSymbols.size) { nullableAnyType },
                ).toFirResolvedTypeRef(null)
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

        return if (firReplStateFromDependencies == null && explicitClassId == null) {
            // Not on the classpath (no genuinely earlier compile call created it), not cached
            // either (no same-batch sibling created it moments ago), and no externally-provided
            // state object was configured -- this really is the very first snippet ever for this
            // state, so build and schedule it for codegen now.
            classifierStorage.createAndCacheIrClass(firReplStateObject, irSnippet.parent).also { irReplStateObject ->
                classifiersGenerator.processClassHeader(firReplStateObject, irReplStateObject)
                declarationStorage.createAndCacheIrConstructor(
                    firReplStateObject.declarations.filterIsInstance<FirPrimaryConstructor>().first(),
                    { irReplStateObject }, isLocal = false
                )
                firReplStateObject.accept(fir2IrVisitor, null)
            }
        } else {
            val irReplStateParent =
                declarationStorage.getIrExternalPackageFragment(firReplStateObject.symbol.classId.packageFqName, session.moduleData)
            lazyDeclarationsGenerator.createIrLazyClass(firReplStateObject, irReplStateParent, IrClassSymbolImpl())
        }.also { cachedStateObjectIrClass = it }
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
        fun FirBasedSymbol<FirDeclaration>.getOriginalSnippetSymbol(): FirReplSnippetSymbol? = fir.originalReplSnippetSymbol

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
        resolvedTypeRef.coneType.toClassSymbol(session)?.let { storeAccessedSymbol(it) }
        resolvedTypeRef.coneType.typeArguments.forEach {
            it.type?.toClassSymbol(session)?.let { storeAccessedSymbol(it) }
        }
    }
}
