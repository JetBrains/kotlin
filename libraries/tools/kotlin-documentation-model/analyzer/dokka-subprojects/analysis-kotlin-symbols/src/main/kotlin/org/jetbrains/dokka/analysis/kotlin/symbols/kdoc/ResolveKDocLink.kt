/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Util to print a message about unresolved [link]
 */
internal fun DokkaLogger.logUnresolvedLink(link: String, location: String?) {
    warn("Couldn't resolve link for $link" + if (location != null) " in $location" else "")
}

internal inline fun DRI?.ifUnresolved(action: () -> Unit): DRI? = this ?: run {
    action()
    null
}

/**
 * Resolves KDoc link via creating PSI.
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [DRI] or null if the [link] is unresolved
 */
internal fun KtAnalysisSession.resolveKDocTextLinkToDRI(link: String, context: PsiElement? = null): DRI? {
    val kDocLink = createKDocLink(link, context)
    return kDocLink?.let { resolveKDocLinkToDRI(it) }
}

/**
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [KtSymbol] or null if the [link] is unresolved
 */
internal fun KtAnalysisSession.resolveKDocTextLinkToSymbol(link: String, context: PsiElement? = null): KtSymbol? {
    val kDocLink = createKDocLink(link, context)
    return kDocLink?.let { resolveToSymbol(it) }
}

private fun KtAnalysisSession.createKDocLink(link: String, context: PsiElement? = null): KDocLink? {
    val psiFactory = context?.let { KtPsiFactory.contextual(it) } ?: KtPsiFactory(this.useSiteModule.project)
    val kDoc = psiFactory.createComment(
        """
    /**
    * [$link]
    */
 """.trimIndent()
    ) as? KDoc

    return kDoc?.getDefaultSection()?.children?.filterIsInstance<KDocLink>()?.singleOrNull()
}

/**
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [DRI] or null if the [link] is unresolved
 */
internal fun KtAnalysisSession.resolveKDocLinkToDRI(link: KDocLink): DRI? {
    val linkedSymbol = resolveToSymbol(link)
    return if (linkedSymbol == null) null
    else getDRIFromSymbol(linkedSymbol)
}

private fun KtAnalysisSession.resolveToSymbol(kDocLink: KDocLink): KtSymbol? {
    val lastNameSegment = kDocLink.children.filterIsInstance<KDocName>().lastOrNull()
    return lastNameSegment?.mainReference?.resolveToSymbols()?.sortedWith(linkCandidatesComparator)?.firstOrNull()
}

/**
 * The order is like in [K1](https://github.com/JetBrains/intellij-community/blob/84f54ed97da66d6e24e6572345869bf1071945b6/plugins/kotlin/base/fe10/kdoc/src/org/jetbrains/kotlin/idea/kdoc/resolveKDocLink.kt#L104)
 *
 * TODO KT-64190
 */
private var linkCandidatesComparator: Comparator<KtSymbol> = compareBy{
    when(it) {
        is KtClassifierSymbol -> 1
        is KtPackageSymbol -> 2
        is KtFunctionSymbol -> 3
        is KtVariableSymbol-> 4
        else -> 5
    }
}