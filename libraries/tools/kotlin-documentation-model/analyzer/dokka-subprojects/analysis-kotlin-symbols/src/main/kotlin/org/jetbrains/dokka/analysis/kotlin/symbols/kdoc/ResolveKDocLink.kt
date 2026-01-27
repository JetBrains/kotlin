/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.getLocation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Resolves a KDoc link, logging a warning in case of unresolved links.
 *
 * For the resolution logic, see [resolveKDocLinkToDRI]
 */
internal fun resolveKDocLink(
    link: KDocLink,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): DRI? {
    val dri = resolveKDocLinkToDRI(link)
    if (dri == null) {
        logUnresolvedLink(link.getLinkText(),  getLocation(link), logger, sourceSet)
    }
    return dri
}

/**
 * Resolves a KDoc link from text representation, logging a warning in case of unresolved links.
 *
 * For the resolution logic, see [resolveKDocTextLinkToDRI]
 */
internal fun KaSession.resolveKDocTextLink(
    link: String,
    contextPackageFQN: String?,
    location: String?,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): DRI? {
    val dri = resolveKDocTextLinkToDRI(link, contextPackageFQN)
    if (dri == null) {
        logUnresolvedLink(link, location, logger, sourceSet)
    }
    return dri
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
 * @return [DRI] or null if the [link] is unresolved
 */
internal fun KaSession.resolveKDocTextLinkToDRI(link: String, contextPackageFQN: String? = null): DRI? {
    val kDocLink = createKDocLink(link, contextPackageFQN)
    return kDocLink?.let { resolveKDocLinkToDRI(it) }
}

/**
 * Resolves KDoc link via creating PSI in the context of a module provided by [KaSession]
 *
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [KaSymbol] or null if the [link] is unresolved
 */
internal fun KaSession.resolveKDocTextLinkToSymbol(link: String): KaSymbol? {
    val kDocLink = createKDocLink(link, null)
    return kDocLink?.let {
        /**
         *  Get [KaSession] is associated with [a dangling module][org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule]
         */
        analyze(kDocLink) { resolveToSymbol(it) }
    }
}

private fun KaSession.createKDocLink(link: String, contextPackageFQN: String?): KDocLink? {
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
 * @return [DRI] or null if the [kDocLink] is unresolved
 */
private fun resolveKDocLinkToDRI(kDocLink: KDocLink): DRI? {
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
        else getDRIFromSymbol(linkedSymbol)
    }
}

private fun KaSession.resolveToSymbol(kDocLink: KDocLink): KaSymbol? {
    val lastNameSegment = kDocLink.children.filterIsInstance<KDocName>().lastOrNull()
    return lastNameSegment?.mainReference?.resolveToSymbols()?.sortedWith(linkCandidatesComparator)?.firstOrNull()
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