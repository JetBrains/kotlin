/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.checkDecompiledText

internal fun parseFromKDocTag(
    kDocTag: KDocTag?,
    externalDri: (KDocLink) -> DRI?,
    kdocLocation: String?,
    parseWithChildren: Boolean = true
): DocumentationNode {
    return if (kDocTag == null) {
        DocumentationNode(emptyList())
    } else {
        fun parseStringToDocNode(text: String, externalDRIProvider: (String) -> DRI?) =
            MarkdownParser(externalDRIProvider, kdocLocation).parseStringToDocNode(text)

        fun pointedLink(tag: KDocTag): DRI? = tag.getSubjectLink()?.let(externalDri)

        val allTags =
            listOf(kDocTag) + if (kDocTag.canHaveParent() && parseWithChildren) getAllKDocTags(findParent(kDocTag)) else emptyList()
        DocumentationNode(
            allTags.map { tag ->
                val links = mutableMapOf<String, KDocLink>()
                tag.forEachDescendantOfType<KDocLink>{
                    links[it.getLinkText()] = it
                }

                val externalDRIProvider = { linkText: String -> links[linkText]?.let { externalDri(it) } }

                when (tag.knownTag) {
                    null -> if (tag.name == null) Description(parseStringToDocNode(tag.getContent(), externalDRIProvider)) else CustomTagWrapper(
                        parseStringToDocNode(tag.getContent(), externalDRIProvider),
                        tag.name!!
                    )
                    KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                    KDocKnownTag.THROWS -> {
                        Throws(
                            parseStringToDocNode(tag.getContent(), externalDRIProvider),
                            tag.getSubjectName().orEmpty(),
                            pointedLink(tag),
                        )
                    }
                    KDocKnownTag.EXCEPTION -> {
                        Throws(
                            parseStringToDocNode(tag.getContent(), externalDRIProvider),
                            tag.getSubjectName().orEmpty(),
                            pointedLink(tag)
                        )
                    }
                    KDocKnownTag.PARAM -> Param(
                        parseStringToDocNode(tag.getContent(), externalDRIProvider),
                        tag.getSubjectName().orEmpty(),
                        pointedLink(tag),
                    )
                    KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                    KDocKnownTag.RETURN -> Return(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                    KDocKnownTag.SEE -> {
                        See(
                            parseStringToDocNode(tag.getContent(), externalDRIProvider),
                            tag.getSubjectName().orEmpty(),
                            pointedLink(tag),
                        )
                    }
                    KDocKnownTag.SINCE -> Since(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                    KDocKnownTag.CONSTRUCTOR -> Constructor(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                    KDocKnownTag.PROPERTY -> Property(
                        parseStringToDocNode(tag.getContent(), externalDRIProvider),
                        tag.getSubjectName().orEmpty()
                    )
                    KDocKnownTag.SAMPLE -> Sample(
                        /**
                         * All links should be left unresolved in a @sample doc section
                         * Further they should be resolved by DefaultSamplesTransformer
                         */
                        parseStringToDocNode(tag.getContent()) { null },
                        tag.getSubjectName().orEmpty()
                    )
                    KDocKnownTag.SUPPRESS -> Suppress(parseStringToDocNode(tag.getContent(), externalDRIProvider))
                }
            }
        )
    }
}

private fun findParent(kDoc: PsiElement): PsiElement =
    if (kDoc.canHaveParent()) findParent(kDoc.parent) else kDoc

private fun PsiElement.canHaveParent(): Boolean = this is KDocSection && knownTag != KDocKnownTag.PROPERTY

private fun getAllKDocTags(kDocImpl: PsiElement): List<KDocTag> =
    kDocImpl.children.filterIsInstance<KDocTag>().filterNot { it is KDocSection } + kDocImpl.children.flatMap {
        getAllKDocTags(it)
    }

// copied-pasted from [org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType]
private inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

private inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit
) {
    checkDecompiledText()
    this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (canGoInside(element)) {
                super.visitElement(element)
            }

            if (element is T) {
                action(element)
            }
        }
    })
}