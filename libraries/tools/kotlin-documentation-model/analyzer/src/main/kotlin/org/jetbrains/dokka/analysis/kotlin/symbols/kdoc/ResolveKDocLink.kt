/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.kotlin.documentation.analyzer.ids.contextParameterDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.generateDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.receiverDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.typeParameterDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.valueParameterDeclarationId
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.getLocation
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulSymbols
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.documentation.analyzer.ids.UNRESOLVED_DECLARATION_ID
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Resolves a KDoc link to the target's declaration ID (see `generateDeclarationId`), logging a warning in
 * case of unresolved links.
 *
 * For the resolution logic, see [resolveKDocLinkToDeclarationId]
 */
internal fun resolveKDocLink(
    link: KDocLink,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdElementId? {
    val declarationId = resolveKDocLinkToDeclarationId(link)
    if (declarationId == null) {
        logUnresolvedLink(link.getLinkText(),  getLocation(link), logger, sourceSet)
    }
    return declarationId
}

/**
 * Resolves a KDoc link from text representation, logging a warning in case of unresolved links.
 *
 * For the resolution logic, see [resolveKDocTextLinkToDeclarationId]
 */
context(_: KaSession)
internal fun resolveKDocTextLink(
    link: String,
    contextPackageFQN: String?,
    location: String?,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdElementId? {
    val declarationId = resolveKDocTextLinkToDeclarationId(link, contextPackageFQN)
    if (declarationId == null) {
        logUnresolvedLink(link, location, logger, sourceSet)
    }
    return declarationId
}

/**
 * Util to print a message about unresolved [link]
 */
private fun logUnresolvedLink(
    link: String,
    location: String?,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
) {
    logger.warn("Couldn't resolve link: [$link]" + (if (location != null) " in $location" else "") + " (${sourceSet.sourceSetID})")
}

/**
 * Resolves KDoc link via creating PSI in the context of a module provided by [KaSession]
 *
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @param contextPackageFQN FQN of a context package
 * @return the target's declaration ID or null if the [link] is unresolved
 */
context(_: KaSession)
internal fun resolveKDocTextLinkToDeclarationId(link: String, contextPackageFQN: String? = null): KdElementId? {
    val kDocLink = createKDocLink(link, contextPackageFQN)
    return kDocLink?.let { resolveKDocLinkToDeclarationId(it) }
}

/**
 * Resolves KDoc link via creating PSI in the context of a module provided by [KaSession]
 *
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [KaSymbol] or null if the [link] is unresolved
 */
context(_: KaSession)
internal fun resolveKDocTextLinkToSymbol(link: String): KaSymbol? {
    val kDocLink = createKDocLink(link, null)
    return kDocLink?.let {
        /**
         *  Get [KaSession] is associated with [a dangling module][org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule]
         */
        analyze(kDocLink) { resolveToSymbol(it) }
    }
}

context(_: KaSession)
private fun createKDocLink(link: String, contextPackageFQN: String?): KDocLink? {
    val currentModule: KaSourceModule = useSiteModule as? KaSourceModule
        ?: throw IllegalStateException("Resolving KDoc links can be done only in a source module, not $useSiteModule")

    // creates dummy.kt file
    val dummyFileText = if (!contextPackageFQN.isNullOrBlank()) """
    package $contextPackageFQN
    
    /**
    * [$link]
    */
    """.trimIndent()
    else """
    /**
    * [$link]
    */
    """

    /**
     * Creates a dangling file stored in-memory
     * Such file belongs to [a separate module][org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule]
     *
     * Additional information: https://kotlin.github.io/analysis-api/in-memory-file-analysis.html#stand-alone-file-analysis
     */
    val dummyFile = KtPsiFactory(currentModule.project).createFile(dummyFileText)

    // pass context (dependencies) from the current source module to a dangling module,
    // by default, the file will have no context,
    // and so will it will not be possible to resolve declarations from outside the file itself.
    @OptIn(KaExperimentalApi::class)
    dummyFile.contextModule = currentModule

    val comments = dummyFile.children.filterIsInstance<KDoc>()
    val kDoc = comments.single()

    return kDoc.getDefaultSection().children.filterIsInstance<KDocLink>().singleOrNull()
}

/**
 * If the [kDocLink] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return the target's declaration ID or null if the [kDocLink] is unresolved
 */
private fun resolveKDocLinkToDeclarationId(kDocLink: KDocLink): KdElementId? {
    /**
     * [kDocLink] can belong to [a dangling module][org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule]
     * or [a source module][org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule]
     *
     * Since [KaSession] is associated with a specific module,
     * [analyze] should be called to get a corresponding instance of [KaSession]
     */
    analyze(kDocLink) {
        val linkedSymbol = resolveToSymbol(kDocLink)
        return if (linkedSymbol == null) null
        else declarationIdFrom(linkedSymbol)
    }
}

/**
 * The declaration ID of [symbol] as a KDoc link target. The owning declaration is determined first and
 * gets its ID from [generateDeclarationId]; non-declaration symbols (value/context parameters, type
 * parameters, receivers) append their owner-relative part to the owner's ID with the ID helpers, e.g.
 * [valueParameterDeclarationId]. Local and unsupported symbols resolve to [UNRESOLVED_DECLARATION_ID].
 */
context(session: KaSession)
private fun declarationIdFrom(symbol: KaSymbol): KdElementId {
    val owner = ownerSymbolOf(symbol) ?: return UNRESOLVED_DECLARATION_ID
    val ownerId = generateDeclarationIdOrNull(owner) ?: return UNRESOLVED_DECLARATION_ID
    return when {
        symbol === owner -> ownerId
        symbol is KaValueParameterSymbol -> valueParameterDeclarationId(
            ownerId,
            (owner as? KaFunctionSymbol)?.valueParameters?.indexOf(symbol)
                ?: error("Value parameter `$symbol` is not owned by a function: $owner"),
        )
        symbol is KaContextParameterSymbol -> contextParameterDeclarationId(
            ownerId,
            (owner as? KaCallableSymbol)?.contextParameters?.indexOf(symbol)
                ?: error("Context parameter `$symbol` is not owned by a callable: $owner"),
        )
        symbol is KaTypeParameterSymbol -> typeParameterDeclarationId(
            ownerId,
            (owner as? KaDeclarationSymbol)?.typeParameters?.indexOf(symbol)
                ?: error("Type parameter `$symbol` is not owned by a declaration: $owner"),
        )
        symbol is KaReceiverParameterSymbol -> receiverDeclarationId(ownerId)
        else -> ownerId
    }
}

/**
 * The nearest declaration owning [symbol] in a KDoc link target: parameters and receivers walk up
 * through their containing symbols until a classlike, callable, or package symbol is reached.
 */
context(_: KaSession)
private fun ownerSymbolOf(symbol: KaSymbol): KaSymbol? {
    var current: KaSymbol = symbol
    while (true) {
        current = when (current) {
            // Value/context parameters are KaCallableSymbols but have no ID of their own,
            // so they must be walked up before the callable branch below.
            is KaValueParameterSymbol, is KaContextParameterSymbol, is KaTypeParameterSymbol,
            is KaReceiverParameterSymbol -> current.containingSymbol ?: return null
            is KaClassLikeSymbol, is KaCallableSymbol, is KaPackageSymbol -> return current
            is KaDeclarationSymbol -> current.containingSymbol ?: return null
            else -> return null
        }
    }
}

/** [generateDeclarationId] that yields `null` for local and unsupported symbols instead of throwing. */
context(session: KaSession)
private fun generateDeclarationIdOrNull(symbol: KaSymbol): KdElementId? = try {
    generateDeclarationId(symbol)
} catch (_: IllegalStateException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

context(_: KaSession)
private fun resolveToSymbol(kDocLink: KDocLink): KaSymbol? {
    val lastNameSegment = kDocLink.children.filterIsInstance<KDocName>().lastOrNull()
    @OptIn(KaExperimentalApi::class, KtExperimentalApi::class)
    return lastNameSegment?.resolveSuccessfulSymbols()?.sortedWith(linkCandidatesComparator)?.firstOrNull()
}

/**
 * The order is like in [K1](https://github.com/JetBrains/intellij-community/blob/84f54ed97da66d6e24e6572345869bf1071945b6/plugins/kotlin/base/fe10/kdoc/src/org/jetbrains/kotlin/idea/kdoc/resolveKDocLink.kt#L104)
 *
 * TODO KT-64190
 */
private var linkCandidatesComparator: Comparator<KaSymbol> = compareBy {
    when (it) {
        is KaClassifierSymbol -> 1
        is KaPackageSymbol -> 2
        is KaFunctionSymbol -> 3
        is KaVariableSymbol -> 4
        else -> 5
    }
}
