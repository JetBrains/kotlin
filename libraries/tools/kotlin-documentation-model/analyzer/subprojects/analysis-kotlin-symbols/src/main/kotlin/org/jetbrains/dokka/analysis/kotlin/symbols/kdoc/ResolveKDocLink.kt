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

internal fun DRI?.logIfNotResolved(link: String, logger: DokkaLogger): DRI? {
    if(this == null)
    logger.warn("Couldn't resolve link for $link")
    return this
}

/**
 * It resolves KDoc link via creating PSI.
 *
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

internal fun KtAnalysisSession.resolveKDocLink(link: KDocLink): DRI? {
    val lastNameSegment = link.children.filterIsInstance<KDocName>().lastOrNull()
    val linkedSymbol = lastNameSegment?.mainReference?.resolveToSymbols()?.firstOrNull()
    return if (linkedSymbol == null) null // logger.warn("Couldn't resolve link for $link")
    else getDRIFromSymbol(linkedSymbol)
}