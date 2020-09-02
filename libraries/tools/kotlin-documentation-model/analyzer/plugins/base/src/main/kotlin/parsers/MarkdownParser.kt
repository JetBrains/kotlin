package org.jetbrains.dokka.base.parsers

import com.intellij.psi.PsiElement
import org.intellij.markdown.MarkdownElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.impl.ListItemCompositeNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.parsers.factories.DocTagsFromIElementFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import java.net.MalformedURLException
import java.net.URL
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

class MarkdownParser(
    private val resolutionFacade: DokkaResolutionFacade? = null,
    private val declarationDescriptor: DeclarationDescriptor? = null,
    private val logger: DokkaLogger
) : Parser() {

    inner class MarkdownVisitor(val text: String, val destinationLinksMap: Map<String, String>) {

        private fun headersHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type,
                visitNode(node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                    ?: throw IllegalStateException("Wrong AST Tree. ATX Header does not contain expected content")).children
            )

        private fun horizontalRulesHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(MarkdownTokenTypes.HORIZONTAL_RULE)

        private fun emphasisHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type,
                children = node.children.evaluateChildrenWithDroppedEnclosingTokens(1)
            )

        private fun strongHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type,
                children = node.children.evaluateChildrenWithDroppedEnclosingTokens(2)
            )

        private fun List<ASTNode>.evaluateChildrenWithDroppedEnclosingTokens(count: Int) =
            drop(count).dropLast(count).evaluateChildren()

        private fun blockquotesHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type, children = node.children
                    .filterIsInstance<CompositeASTNode>()
                    .evaluateChildren()
            )

        private fun listsHandler(node: ASTNode): DocTag {

            val children = node.children.filterIsInstance<ListItemCompositeNode>().flatMap {
                if (it.children.last().type in listOf(
                        MarkdownElementTypes.ORDERED_LIST,
                        MarkdownElementTypes.UNORDERED_LIST
                    )
                ) {
                    val nestedList = it.children.last()
                    (it.children as MutableList).removeAt(it.children.lastIndex)
                    listOf(it, nestedList)
                } else
                    listOf(it)
            }

            return DocTagsFromIElementFactory.getInstance(
                node.type,
                children =
                children
                    .map {
                        if (it.type == MarkdownElementTypes.LIST_ITEM)
                            DocTagsFromIElementFactory.getInstance(
                                it.type,
                                children = it
                                    .children
                                    .filterIsInstance<CompositeASTNode>()
                                    .evaluateChildren()
                            )
                        else
                            visitNode(it)
                    },
                params =
                if (node.type == MarkdownElementTypes.ORDERED_LIST) {
                    val listNumberNode = node.children.first().children.first()
                    mapOf(
                        "start" to text.substring(
                            listNumberNode.startOffset,
                            listNumberNode.endOffset
                        ).trim().dropLast(1)
                    )
                } else
                    emptyMap()
            )
        }

        private fun resolveDRI(mdLink: String): DRI? =
            mdLink
                .removePrefix("[")
                .removeSuffix("]")
                .let { link ->
                    try {
                        URL(link)
                        null
                    } catch (e: MalformedURLException) {
                        try {
                            if (resolutionFacade != null && declarationDescriptor != null) {
                                resolveKDocLink(
                                    resolutionFacade.resolveSession.bindingContext,
                                    resolutionFacade,
                                    declarationDescriptor,
                                    null,
                                    link.split('.')
                                ).sorted().firstOrNull()?.let { DRI.from(it) }
                            } else null
                        } catch (e1: IllegalArgumentException) {
                            logger.warn("Couldn't resolve link for $mdLink")
                            null
                        }
                    }
                }

        private fun Collection<DeclarationDescriptor>.sorted() = sortedWith(
            compareBy(
                { it is ClassDescriptor },
                { (it as? FunctionDescriptor)?.name },
                { (it as? FunctionDescriptor)?.valueParameters?.size },
                { (it as? FunctionDescriptor)?.valueParameters?.joinToString { it.type.toString() } }
            )
        )

        private fun referenceLinksHandler(node: ASTNode): DocTag {
            val linkLabel = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }
                ?: throw IllegalStateException("Wrong AST Tree. Reference link does not contain expected content")
            val linkText = node.children.findLast { it.type == MarkdownElementTypes.LINK_TEXT } ?: linkLabel

            val linkKey = text.substring(linkLabel.startOffset, linkLabel.endOffset)

            val link = destinationLinksMap[linkKey.toLowerCase()] ?: linkKey

            return linksHandler(linkText, link)
        }

        private fun inlineLinksHandler(node: ASTNode): DocTag {
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                ?: throw IllegalStateException("Wrong AST Tree. Inline link does not contain expected content")
            val linkDestination = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                ?: throw IllegalStateException("Wrong AST Tree. Inline link does not contain expected content")
            val linkTitle = node.children.find { it.type == MarkdownElementTypes.LINK_TITLE }

            val link = text.substring(linkDestination.startOffset, linkDestination.endOffset)

            return linksHandler(linkText, link, linkTitle)
        }

        private fun autoLinksHandler(node: ASTNode): DocTag {
            val link = text.substring(node.startOffset + 1, node.endOffset - 1)

            return linksHandler(node, link)
        }

        private fun linksHandler(linkText: ASTNode, link: String, linkTitle: ASTNode? = null): DocTag {
            val dri: DRI? = resolveDRI(link)
            val linkTextString =
                if (linkTitle == null) link else text.substring(linkTitle.startOffset + 1, linkTitle.endOffset - 1)

            val params = if (linkTitle == null)
                mapOf("href" to link)
            else
                mapOf("href" to link, "title" to linkTextString)


            return if (dri == null && !link.isRemoteLink()) {
                DocTagsFromIElementFactory.getInstance(
                    MarkdownTokenTypes.TEXT,
                    params = params,
                    children = linkText.children.drop(1).dropLast(1).evaluateChildren(),
                    body = linkTextString.removeSurrounding("[", "]")
                )
            } else {
                DocTagsFromIElementFactory.getInstance(
                    MarkdownElementTypes.INLINE_LINK,
                    params = params,
                    children = linkText.children.drop(1).dropLast(1).evaluateChildren(),
                    dri = dri
                )
            }
        }

        private fun String.isRemoteLink() = try {
            URL(this)
            true
        } catch (e: MalformedURLException) {
            false
        }

        private fun imagesHandler(node: ASTNode): DocTag =
            with(node.children.last().children) {
                val destination = find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                val description = find { it.type == MarkdownElementTypes.LINK_TEXT }

                val src = destination?.let {
                    mapOf("href" to text.substring(it.startOffset, it.endOffset))
                } ?: emptyMap()

                val alt = description?.let {
                    mapOf("alt" to text.substring(it.startOffset + 1, it.endOffset - 1))
                } ?: emptyMap()

                return DocTagsFromIElementFactory.getInstance(
                    node.type,
                    params = src + alt
                )
            }

        private fun codeSpansHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type,
                children = listOf(
                    DocTagsFromIElementFactory.getInstance(
                        MarkdownTokenTypes.TEXT,
                        body = text.substring(node.startOffset + 1, node.endOffset - 1).replace('\n', ' ').trimIndent()
                    )

                )
            )

        private fun codeFencesHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                node.type,
                children = node
                    .children
                    .dropWhile { it.type != MarkdownTokenTypes.CODE_FENCE_CONTENT }
                    .dropLastWhile { it.type != MarkdownTokenTypes.CODE_FENCE_CONTENT }
                    .map {
                        if (it.type == MarkdownTokenTypes.EOL)
                            LeafASTNode(MarkdownTokenTypes.HARD_LINE_BREAK, 0, 0)
                        else
                            it
                    }.evaluateChildren(),
                params = node
                    .children
                    .find { it.type == MarkdownTokenTypes.FENCE_LANG }
                    ?.let { mapOf("lang" to text.substring(it.startOffset, it.endOffset)) }
                    ?: emptyMap()
            )

        private fun codeBlocksHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(node.type, children = node.children.mergeLeafASTNodes().map {
                DocTagsFromIElementFactory.getInstance(
                    MarkdownTokenTypes.TEXT,
                    body = text.substring(it.startOffset, it.endOffset)
                )
            })

        private fun defaultHandler(node: ASTNode): DocTag =
            DocTagsFromIElementFactory.getInstance(
                MarkdownElementTypes.PARAGRAPH,
                children = node.children.evaluateChildren()
            )

        fun visitNode(node: ASTNode): DocTag =
            when (node.type) {
                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4,
                MarkdownElementTypes.ATX_5,
                MarkdownElementTypes.ATX_6 -> headersHandler(node)
                MarkdownTokenTypes.HORIZONTAL_RULE -> horizontalRulesHandler(node)
                MarkdownElementTypes.STRONG -> strongHandler(node)
                MarkdownElementTypes.EMPH -> emphasisHandler(node)
                MarkdownElementTypes.FULL_REFERENCE_LINK,
                MarkdownElementTypes.SHORT_REFERENCE_LINK -> referenceLinksHandler(node)
                MarkdownElementTypes.INLINE_LINK -> inlineLinksHandler(node)
                MarkdownElementTypes.AUTOLINK -> autoLinksHandler(node)
                MarkdownElementTypes.BLOCK_QUOTE -> blockquotesHandler(node)
                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST -> listsHandler(node)
                MarkdownElementTypes.CODE_BLOCK -> codeBlocksHandler(node)
                MarkdownElementTypes.CODE_FENCE -> codeFencesHandler(node)
                MarkdownElementTypes.CODE_SPAN -> codeSpansHandler(node)
                MarkdownElementTypes.IMAGE -> imagesHandler(node)
                MarkdownTokenTypes.HARD_LINE_BREAK -> DocTagsFromIElementFactory.getInstance(node.type)
                MarkdownTokenTypes.CODE_FENCE_CONTENT,
                MarkdownTokenTypes.CODE_LINE -> DocTagsFromIElementFactory.getInstance(
                    MarkdownElementTypes.CODE_BLOCK,
                    body = text.substring(node.startOffset, node.endOffset)
                )
                MarkdownTokenTypes.TEXT -> DocTagsFromIElementFactory.getInstance(
                    MarkdownTokenTypes.TEXT,
                    body = text.substring(node.startOffset, node.endOffset).transform()
                )
                MarkdownElementTypes.MARKDOWN_FILE -> if (node.children.size == 1) visitNode(node.children.first()) else defaultHandler(
                    node
                )
                GFMElementTypes.STRIKETHROUGH -> DocTagsFromIElementFactory.getInstance(
                    GFMElementTypes.STRIKETHROUGH,
                    body = text
                        .substring(node.startOffset, node.endOffset).transform()
                )
                GFMElementTypes.TABLE -> DocTagsFromIElementFactory.getInstance(
                    GFMElementTypes.TABLE,
                    children = node.children.filterTabSeparators().evaluateChildren()
                )
                GFMElementTypes.HEADER -> DocTagsFromIElementFactory.getInstance(
                    GFMElementTypes.HEADER,
                    children = node.children.filterTabSeparators().evaluateChildren()
                )
                GFMElementTypes.ROW -> DocTagsFromIElementFactory.getInstance(
                    GFMElementTypes.ROW,
                    children = node.children.filterTabSeparators().evaluateChildren()
                )
                else -> defaultHandler(node)
            }

        private fun List<ASTNode>.filterTabSeparators() =
            this.filterNot { it.type == GFMTokenTypes.TABLE_SEPARATOR }

        private fun List<ASTNode>.evaluateChildren(): List<DocTag> =
            this.removeUselessTokens().mergeLeafASTNodes().map { visitNode(it) }

        private fun List<ASTNode>.removeUselessTokens(): List<ASTNode> =
            this.filterIndexed { index, node ->
                !(node.type == MarkdownElementTypes.LINK_DEFINITION || (
                        node.type == MarkdownTokenTypes.EOL &&
                                this.getOrNull(index - 1)?.type == MarkdownTokenTypes.HARD_LINE_BREAK
                        ))
            }

        private val notLeafNodes = listOf(MarkdownTokenTypes.HORIZONTAL_RULE, MarkdownTokenTypes.HARD_LINE_BREAK)

        private fun List<ASTNode>.isNotLeaf(index: Int): Boolean =
            if (index in 0..this.lastIndex)
                (this[index] is CompositeASTNode) || this[index].type in notLeafNodes
            else
                false

        private fun List<ASTNode>.mergeLeafASTNodes(): List<ASTNode> {
            val children: MutableList<ASTNode> = mutableListOf()
            var index = 0
            while (index <= this.lastIndex) {
                if (this.isNotLeaf(index)) {
                    children += this[index]
                } else {
                    val startOffset = this[index].startOffset
                    val sIndex = index
                    while (index < this.lastIndex) {
                        if (this.isNotLeaf(index + 1) || this[index + 1].startOffset != this[index].endOffset) {
                            mergedLeafNode(this, index, startOffset, sIndex)?.run {
                                children += this
                            }
                            break
                        }
                        index++
                    }
                    if (index == this.lastIndex) {
                        mergedLeafNode(this, index, startOffset, sIndex)?.run {
                            children += this
                        }
                    }
                }
                index++
            }
            return children
        }

        private fun mergedLeafNode(nodes: List<ASTNode>, index: Int, startOffset: Int, sIndex: Int): LeafASTNode? {
            val endOffset = nodes[index].endOffset
            if (text.substring(startOffset, endOffset).transform().trim().isNotEmpty()) {
                val type = if (nodes.subList(sIndex, index)
                        .any { it.type == MarkdownTokenTypes.CODE_LINE }
                ) MarkdownTokenTypes.CODE_LINE else MarkdownTokenTypes.TEXT
                return LeafASTNode(type, startOffset, endOffset)
            }
            return null
        }

        private fun String.transform() = this
            .replace(Regex("\n\n+"), "")        // Squashing new lines between paragraphs
            .replace(Regex("\n"), " ")
            .replace(Regex(" >+ +"), " ")      // Replacement used in blockquotes, get rid of garbage
    }


    private fun getAllDestinationLinks(text: String, node: ASTNode): List<Pair<String, String>> =
        node.children
            .filter { it.type == MarkdownElementTypes.LINK_DEFINITION }
            .map {
                text.substring(it.children[0].startOffset, it.children[0].endOffset).toLowerCase() to
                        text.substring(it.children[2].startOffset, it.children[2].endOffset)
            } +
                node.children.filterIsInstance<CompositeASTNode>().flatMap { getAllDestinationLinks(text, it) }


    private fun markdownToDocNode(text: String): DocTag {

        val flavourDescriptor = GFMFlavourDescriptor()
        val markdownAstRoot: ASTNode = IntellijMarkdownParser(flavourDescriptor).buildMarkdownTreeFromString(text)

        return MarkdownVisitor(text, getAllDestinationLinks(text, markdownAstRoot).toMap()).visitNode(markdownAstRoot)
    }

    override fun parseStringToDocNode(extractedString: String) = markdownToDocNode(extractedString)
    override fun preparse(text: String) = text

    private fun findParent(kDoc: PsiElement): PsiElement =
        if (kDoc is KDocSection) findParent(kDoc.parent) else kDoc

    private fun getAllKDocTags(kDocImpl: PsiElement): List<KDocTag> =
        kDocImpl.children.filterIsInstance<KDocTag>().filterNot { it is KDocSection } + kDocImpl.children.flatMap {
            getAllKDocTags(
                it
            )
        }

    fun parseFromKDocTag(kDocTag: KDocTag?): DocumentationNode {
        return if (kDocTag == null)
            DocumentationNode(emptyList())
        else
            DocumentationNode(
                (listOf(kDocTag) + getAllKDocTags(findParent(kDocTag))).map {
                    when (it.knownTag) {
                        null -> if (it.name == null) Description(parseStringToDocNode(it.getContent())) else CustomTagWrapper(
                            parseStringToDocNode(it.getContent()),
                            it.name!!
                        )
                        KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.THROWS -> Throws(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty()
                        )
                        KDocKnownTag.EXCEPTION -> Throws(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty()
                        )
                        KDocKnownTag.PARAM -> Param(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty()
                        )
                        KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.RETURN -> Return(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.SEE -> See(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty(),
                            parseStringToDocNode("[${it.getSubjectName()}]")
                                .let {
                                    val link = it.children[0]
                                    if (link is DocumentationLink) link.dri
                                    else null
                                }
                        )
                        KDocKnownTag.SINCE -> Since(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.CONSTRUCTOR -> Constructor(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.PROPERTY -> Property(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty()
                        )
                        KDocKnownTag.SAMPLE -> Sample(
                            parseStringToDocNode(it.getContent()),
                            it.getSubjectName().orEmpty()
                        )
                        KDocKnownTag.SUPPRESS -> Suppress(parseStringToDocNode(it.getContent()))
                    }
                }
            )
    }
}
