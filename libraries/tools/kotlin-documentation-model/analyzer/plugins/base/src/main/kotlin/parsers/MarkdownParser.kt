package org.jetbrains.dokka.base.parsers

import com.intellij.psi.PsiElement
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.impl.ListItemCompositeNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.jetbrains.dokka.base.parsers.factories.DocTagsFromIElementFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import java.net.MalformedURLException
import java.net.URL
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

open class MarkdownParser(
    private val externalDri: (String) -> DRI?,
    private val kdocLocation: String?
) : Parser() {

    private lateinit var destinationLinksMap: Map<String, String>
    private lateinit var text: String

    override fun parseStringToDocNode(extractedString: String): DocTag {
        val gfmFlavourDescriptor = GFMFlavourDescriptor()
        val markdownAstRoot = IntellijMarkdownParser(gfmFlavourDescriptor).buildMarkdownTreeFromString(extractedString)
        destinationLinksMap = getAllDestinationLinks(extractedString, markdownAstRoot).toMap()
        text = extractedString
        return visitNode(markdownAstRoot)
    }

    override fun preparse(text: String) = text

    override fun parseTagWithBody(tagName: String, content: String): TagWrapper =
        when (tagName) {
            "see" -> {
                val referencedName = content.substringBefore(' ')
                val dri = externalDri(referencedName)
                See(
                    parseStringToDocNode(content.substringAfter(' ')),
                    dri?.fqName() ?: referencedName,
                    dri
                )
            }
            "throws", "exception" -> {
                val dri = externalDri(content.substringBefore(' '))
                Throws(
                    parseStringToDocNode(content.substringAfter(' ')),
                    dri?.fqName() ?: content.substringBefore(' '),
                    dri
                )
            }
            else -> super.parseTagWithBody(tagName, content)
        }

    private fun headersHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            visitNode(node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                ?: throw detailedException("Wrong AST Tree. Header does not contain expected content", node)
            ).children
        )

    private fun horizontalRulesHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(MarkdownTokenTypes.HORIZONTAL_RULE)

    private fun emphasisHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = node.children.evaluateChildrenWithDroppedEnclosingTokens(1)
        )

    private fun strongHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = node.children.evaluateChildrenWithDroppedEnclosingTokens(2)
        )

    private fun List<ASTNode>.evaluateChildrenWithDroppedEnclosingTokens(count: Int) =
        drop(count).dropLast(count).evaluateChildren()

    private fun blockquotesHandler(node: ASTNode) =
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
                    externalDri(link)
                }
            }

    private fun getAllDestinationLinks(text: String, node: ASTNode): List<Pair<String, String>> =
        node.children
            .filter { it.type == MarkdownElementTypes.LINK_DEFINITION }
            .map {
                text.substring(it.children[0].startOffset, it.children[0].endOffset).toLowerCase() to
                        text.substring(it.children[2].startOffset, it.children[2].endOffset)
            } +
                node.children.filterIsInstance<CompositeASTNode>().flatMap { getAllDestinationLinks(text, it) }


    private fun referenceLinksHandler(node: ASTNode): DocTag {
        val linkLabel = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }
            ?: throw detailedException("Wrong AST Tree. Reference link does not contain link label", node)
        val linkText = node.children.findLast { it.type == MarkdownElementTypes.LINK_TEXT } ?: linkLabel

        val linkKey = text.substring(linkLabel.startOffset, linkLabel.endOffset)

        val link = destinationLinksMap[linkKey.toLowerCase()] ?: linkKey

        return linksHandler(linkText, link)
    }

    private fun inlineLinksHandler(node: ASTNode): DocTag {
        val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
            ?: throw detailedException("Wrong AST Tree. Inline link does not contain link text", node)
        val linkDestination = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
        val linkTitle = node.children.find { it.type == MarkdownElementTypes.LINK_TITLE }

        // Link destination may be ommited: https://github.github.com/gfm/#example-495
        val link = linkDestination?.let { text.substring(it.startOffset, it.endOffset) }

        return linksHandler(linkText, link, linkTitle)
    }

    private fun markdownFileHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = node.children.evaluateChildren()
        )

    private fun autoLinksHandler(node: ASTNode): DocTag {
        val link = text.substring(node.startOffset + 1, node.endOffset - 1)

        return linksHandler(node, link)
    }

    private fun linksHandler(linkText: ASTNode, link: String?, linkTitle: ASTNode? = null): DocTag {
        val dri: DRI? = link?.let { resolveDRI(it) }
        val linkOrEmpty = link ?: ""
        val linkTextString =
            if (linkTitle == null) linkOrEmpty else text.substring(linkTitle.startOffset + 1, linkTitle.endOffset - 1)

        val params = if (linkTitle == null)
            mapOf("href" to linkOrEmpty)
        else
            mapOf("href" to linkOrEmpty, "title" to linkTextString)

        return if (link != null && dri == null && !linkOrEmpty.isRemoteLink()) {
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

    private fun codeLineHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        MarkdownElementTypes.CODE_BLOCK,
        body = text.substring(node.startOffset, node.endOffset)
    )

    private fun textHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        MarkdownTokenTypes.TEXT,
        body = text.substring(node.startOffset, node.endOffset).transform()
    )

    private fun strikeThroughHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        node.type,
        children = node.children.evaluateChildrenWithDroppedEnclosingTokens(2)
    )

    private fun tableHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.TABLE,
        children = node.children.filterTabSeparators().evaluateChildren()
    )

    private fun headerHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.HEADER,
        children = node.children.filterTabSeparators().evaluateChildren()
    )

    private fun rowHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.ROW,
        children = node.children.filterTabSeparators().evaluateChildren()
    )

    private fun cellHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMTokenTypes.CELL,
        children = node.children.filterTabSeparators().evaluateChildren().trimSurroundingTokensIfText()
    )

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


    private fun rawHtmlHandler(node: ASTNode): DocTag =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            body = text.substring(node.startOffset, node.endOffset)
        )

    private fun codeSpansHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = listOf(
                DocTagsFromIElementFactory.getInstance(
                    MarkdownTokenTypes.TEXT,
                    body = text.substring(node.startOffset + 1, node.endOffset - 1).replace('\n', ' ').trimIndent()
                )

            )
        )

    private fun codeFencesHandler(node: ASTNode) =
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

    private fun codeBlocksHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(node.type, children = node.children.mergeLeafASTNodes().map {
            DocTagsFromIElementFactory.getInstance(
                MarkdownTokenTypes.TEXT,
                body = text.substring(it.startOffset, it.endOffset)
            )
        })

    private fun defaultHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            MarkdownElementTypes.PARAGRAPH,
            children = node.children.evaluateChildren()
        )

    private fun visitNode(node: ASTNode): DocTag =
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
            MarkdownElementTypes.HTML_BLOCK,
            MarkdownTokenTypes.HTML_TAG,
            MarkdownTokenTypes.HTML_BLOCK_CONTENT -> rawHtmlHandler(node)
            MarkdownTokenTypes.HARD_LINE_BREAK -> DocTagsFromIElementFactory.getInstance(node.type)
            MarkdownTokenTypes.CODE_FENCE_CONTENT,
            MarkdownTokenTypes.CODE_LINE -> codeLineHandler(node)
            MarkdownTokenTypes.TEXT -> textHandler(node)
            MarkdownElementTypes.MARKDOWN_FILE -> markdownFileHandler(node)
            GFMElementTypes.STRIKETHROUGH -> strikeThroughHandler(node)
            GFMElementTypes.TABLE -> tableHandler(node)
            GFMElementTypes.HEADER -> headerHandler(node)
            GFMElementTypes.ROW -> rowHandler(node)
            GFMTokenTypes.CELL -> cellHandler(node)
            else -> defaultHandler(node)
        }

    private fun List<ASTNode>.filterTabSeparators() =
        this.filterNot { it.type == GFMTokenTypes.TABLE_SEPARATOR }

    private fun List<ASTNode>.evaluateChildren(): List<DocTag> =
        this.removeUselessTokens().swapImagesThatShouldBeLinks().mergeLeafASTNodes().map { visitNode(it) }

    private fun List<ASTNode>.swapImagesThatShouldBeLinks(): List<ASTNode> =
        flatMap { node ->
            if (node.type == MarkdownElementTypes.IMAGE
                && node.children.firstOrNull()?.let { it is LeafASTNode && it.type.name == "!" } == true
                && node.children.lastOrNull()?.type == MarkdownElementTypes.SHORT_REFERENCE_LINK
            ) {
                node.children
            } else {
                listOf(node)
            }
        }

    private fun List<ASTNode>.removeUselessTokens(): List<ASTNode> =
        this.filterIndexed { index, node ->
            !(node.type == MarkdownElementTypes.LINK_DEFINITION || (
                    node.type == MarkdownTokenTypes.EOL &&
                            this.getOrNull(index - 1)?.type == MarkdownTokenTypes.HARD_LINE_BREAK
                    ))
        }

    private fun List<DocTag>.trimSurroundingTokensIfText() = mapIndexed { index, elem ->
        val elemTransformed = if (index == 0 && elem is Text) elem.copy(elem.body.trimStart()) else elem
        if (index == lastIndex && elemTransformed is Text) elemTransformed.copy(elemTransformed.body.trimEnd()) else elemTransformed
    }

    private val notLeafNodes = listOf(
        MarkdownTokenTypes.HORIZONTAL_RULE,
        MarkdownTokenTypes.HARD_LINE_BREAK,
        MarkdownTokenTypes.HTML_TAG,
        MarkdownTokenTypes.HTML_BLOCK_CONTENT
    )

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

    private fun detailedException(baseMessage: String, node: ASTNode) =
        IllegalStateException(
            baseMessage + " in ${kdocLocation ?: "unspecified location"}, element starts from offset ${node.startOffset} and ends ${node.endOffset}: ${
                text.substring(
                    node.startOffset,
                    node.endOffset
                )
            }"
        )


    companion object {
        fun parseFromKDocTag(
            kDocTag: KDocTag?,
            externalDri: (String) -> DRI?,
            kdocLocation: String?
        ): DocumentationNode {
            return if (kDocTag == null) {
                DocumentationNode(emptyList())
            } else {
                fun parseStringToDocNode(text: String) =
                    MarkdownParser(externalDri, kdocLocation).parseStringToDocNode(text)

                fun pointedLink(tag: KDocTag): DRI? = (parseStringToDocNode("[${tag.getSubjectName()}]")).let {
                    val link = it.children[0].children[0]
                    if (link is DocumentationLink) link.dri else null
                }

                val allTags =
                    listOf(kDocTag) + if (kDocTag.canHaveParent()) getAllKDocTags(findParent(kDocTag)) else emptyList()
                DocumentationNode(
                    allTags.map {
                        when (it.knownTag) {
                            null -> if (it.name == null) Description(parseStringToDocNode(it.getContent())) else CustomTagWrapper(
                                parseStringToDocNode(it.getContent()),
                                it.name!!
                            )
                            KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(it.getContent()))
                            KDocKnownTag.THROWS -> {
                                val dri = pointedLink(it)
                                Throws(
                                    parseStringToDocNode(it.getContent()),
                                    dri?.fqName() ?: it.getSubjectName().orEmpty(),
                                    dri,
                                )
                            }
                            KDocKnownTag.EXCEPTION -> {
                                val dri = pointedLink(it)
                                Throws(
                                    parseStringToDocNode(it.getContent()),
                                    dri?.fqName() ?: it.getSubjectName().orEmpty(),
                                    dri
                                )
                            }
                            KDocKnownTag.PARAM -> Param(
                                parseStringToDocNode(it.getContent()),
                                it.getSubjectName().orEmpty()
                            )
                            KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(it.getContent()))
                            KDocKnownTag.RETURN -> Return(parseStringToDocNode(it.getContent()))
                            KDocKnownTag.SEE -> {
                                val dri = pointedLink(it)
                                See(
                                    parseStringToDocNode(it.getContent()),
                                    dri?.fqName() ?: it.getSubjectName().orEmpty(),
                                    dri,
                                )
                            }
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

        //Horrible hack but since link resolution is passed as a function i am not able to resolve them otherwise
        fun DRI.fqName(): String? = "$packageName.$classNames".takeIf { packageName != null && classNames != null }

        private fun findParent(kDoc: PsiElement): PsiElement =
            if (kDoc.canHaveParent()) findParent(kDoc.parent) else kDoc

        private fun PsiElement.canHaveParent(): Boolean = this is KDocSection && knownTag != KDocKnownTag.PROPERTY

        private fun getAllKDocTags(kDocImpl: PsiElement): List<KDocTag> =
            kDocImpl.children.filterIsInstance<KDocTag>().filterNot { it is KDocSection } + kDocImpl.children.flatMap {
                getAllKDocTags(it)
            }
    }
}

