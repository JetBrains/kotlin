/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
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
internal fun KtAnalysisSession.resolveKDocTextLink(link: String, context: PsiElement? = null): DRI? {
    val psiFactory = context?.let { KtPsiFactory.contextual(it) } ?: KtPsiFactory(this.useSiteModule.project)
    val kDoc = psiFactory.createComment(
        """
    /**
    * [$link]
    */
 """.trimIndent()
    ) as? KDoc
    val kDocLink = kDoc?.getDefaultSection()?.children?.filterIsInstance<KDocLink>()?.singleOrNull()
    return kDocLink?.let { resolveKDocLink(it) }
}

/**
 * If the [link] is ambiguous, i.e. leads to more than one declaration,
 * it returns deterministically any declaration.
 *
 * @return [DRI] or null if the [link] is unresolved
 */
internal fun KtAnalysisSession.resolveKDocLink(link: KDocLink): DRI? {
    val lastNameSegment = link.children.filterIsInstance<KDocName>().lastOrNull()
    val linkedSymbol = lastNameSegment?.mainReference?.resolveToSymbols()?.firstOrNull()
    return if (linkedSymbol == null) null
    else getDRIFromSymbol(linkedSymbol)
}
