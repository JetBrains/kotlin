/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.impl.ListItemCompositeNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.html.HtmlGenerator
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.markdown.jb.factories.DocTagsFromIElementFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.doc.*
import java.net.MalformedURLException
import java.net.URL
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

@InternalDokkaApi
public open class MarkdownParser(
    private val externalDri: (String) -> DRI?,
    private val kdocLocation: String?,
) : Parser() {

    private lateinit var destinationLinksMap: Map<String, String>
    private lateinit var text: String

    override fun parseStringToDocNode(extractedString: String): DocTag {
        val gfmFlavourDescriptor = GFMFlavourDescriptor()
        val markdownAstRoot = IntellijMarkdownParser(gfmFlavourDescriptor).buildMarkdownTreeFromString(extractedString)
        destinationLinksMap = getAllDestinationLinks(extractedString, markdownAstRoot).toMap()
        text = extractedString

        val parsed = visitNode(markdownAstRoot)
        if (parsed.size == 1) {
            return parsed.first()
        }
        return CustomDocTag(children = parsed, params = emptyMap(), name = "")
    }

    override fun preparse(text: String): String = text.replace("\r\n", "\n").replace("\r", "\n")

    override fun parseTagWithBody(tagName: String, content: String): TagWrapper =
        when (tagName) {
            "see" -> {
                val referencedName = content.substringBefore(' ')
                val dri = externalDri(referencedName)
                See(
                    parseStringToDocNode(content.substringAfter(' ')),
                    dri?.fqDeclarationName() ?: referencedName,
                    dri
                )
            }
            "throws", "exception" -> {
                val dri = externalDri(content.substringBefore(' '))
                Throws(
                    parseStringToDocNode(content.substringAfter(' ')),
                    dri?.fqDeclarationName() ?: content.substringBefore(' '),
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
            ).flatMap { it.children }
        )

    private fun horizontalRulesHandler() =
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

    private fun listsHandler(node: ASTNode): List<DocTag> {

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
                .flatMap {
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


    private fun referenceLinksHandler(node: ASTNode): List<DocTag> {
        val linkLabel = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }
            ?: throw detailedException("Wrong AST Tree. Reference link does not contain link label", node)
        val linkText = node.children.findLast { it.type == MarkdownElementTypes.LINK_TEXT } ?: linkLabel

        val linkKey = text.substring(linkLabel.startOffset, linkLabel.endOffset)

        val link = destinationLinksMap[linkKey.toLowerCase()] ?: linkKey

        return linksHandler(linkText, link)
    }

    private fun inlineLinksHandler(node: ASTNode): List<DocTag> {
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
            children = node.children
                .filterSpacesAndEOL()
                .evaluateChildren()
        )

    private fun autoLinksHandler(node: ASTNode): List<DocTag> {
        val link = text.substring(node.startOffset + 1, node.endOffset - 1)

        return linksHandler(node, link)
    }

    private fun linksHandler(linkText: ASTNode, link: String?, linkTitle: ASTNode? = null): List<DocTag> {
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

    private fun textHandler(node: ASTNode, keepAllFormatting: Boolean) = DocTagsFromIElementFactory.getInstance(
        MarkdownTokenTypes.TEXT,
        body = text.substring(node.startOffset, node.endOffset).transform(),
        keepFormatting = keepAllFormatting
    )

    private fun strikeThroughHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        node.type,
        children = node.children.evaluateChildrenWithDroppedEnclosingTokens(2)
    )

    private fun tableHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.TABLE,
        children = node.children
            .filter { it.type == GFMElementTypes.ROW || it.type == GFMElementTypes.HEADER }
            .evaluateChildren()
    )

    private fun headerHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.HEADER,
        children = node.children
            .filter { it.type == GFMTokenTypes.CELL }
            .evaluateChildren()
    )

    private fun rowHandler(node: ASTNode) = DocTagsFromIElementFactory.getInstance(
        GFMElementTypes.ROW,
        children = node.children
            .filter { it.type == GFMTokenTypes.CELL }
            .evaluateChildren()
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

    private fun imagesHandler(node: ASTNode): List<DocTag> =
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


    private fun rawHtmlHandler(node: ASTNode): List<DocTag> =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            body = text.substring(node.startOffset, node.endOffset)
        )

    private fun codeSpansHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = DocTagsFromIElementFactory.getInstance(
                MarkdownTokenTypes.TEXT,
                body = text.substring(node.startOffset + 1, node.endOffset - 1).replace('\n', ' ').trimIndent(),
                keepFormatting = true
            )
        )

    private fun codeFencesHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            node.type,
            children = node
                .children
                .dropWhile { it.type != MarkdownTokenTypes.CODE_FENCE_CONTENT }
                .dropLastWhile { it.type != MarkdownTokenTypes.CODE_FENCE_CONTENT }
                .filter { it.type != MarkdownTokenTypes.WHITE_SPACE }
                .map {
                    if (it.type == MarkdownTokenTypes.EOL)
                        LeafASTNode(MarkdownTokenTypes.HARD_LINE_BREAK, 0, 0)
                    else
                        it
                }.evaluateChildren(keepAllFormatting = true),
            params = node
                .children
                .find { it.type == MarkdownTokenTypes.FENCE_LANG }
                ?.let { mapOf("lang" to text.substring(it.startOffset, it.endOffset)) }
                ?: emptyMap()
        )

    private fun codeBlocksHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(node.type, children = node.children.mergeLeafASTNodes().flatMap {
            DocTagsFromIElementFactory.getInstance(
                MarkdownTokenTypes.TEXT,
                body = HtmlGenerator.trimIndents(text.substring(it.startOffset, it.endOffset), 4).toString()
            )
        })

    private fun defaultHandler(node: ASTNode) =
        DocTagsFromIElementFactory.getInstance(
            MarkdownElementTypes.PARAGRAPH,
            children = node.children.evaluateChildren()
        )

    private fun visitNode(node: ASTNode, keepAllFormatting: Boolean = false): List<DocTag> =
        when (node.type) {
            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6,
            -> headersHandler(node)
            MarkdownTokenTypes.HORIZONTAL_RULE -> horizontalRulesHandler()
            MarkdownElementTypes.STRONG -> strongHandler(node)
            MarkdownElementTypes.EMPH -> emphasisHandler(node)
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            -> referenceLinksHandler(node)
            MarkdownElementTypes.INLINE_LINK -> inlineLinksHandler(node)
            MarkdownElementTypes.AUTOLINK -> autoLinksHandler(node)
            MarkdownElementTypes.BLOCK_QUOTE -> blockquotesHandler(node)
            MarkdownElementTypes.UNORDERED_LIST,
            MarkdownElementTypes.ORDERED_LIST,
            -> listsHandler(node)
            MarkdownElementTypes.CODE_BLOCK -> codeBlocksHandler(node)
            MarkdownElementTypes.CODE_FENCE -> codeFencesHandler(node)
            MarkdownElementTypes.CODE_SPAN -> codeSpansHandler(node)
            MarkdownElementTypes.IMAGE -> imagesHandler(node)
            MarkdownElementTypes.HTML_BLOCK,
            MarkdownTokenTypes.HTML_TAG,
            MarkdownTokenTypes.HTML_BLOCK_CONTENT,
            -> rawHtmlHandler(node)
            MarkdownTokenTypes.HARD_LINE_BREAK -> DocTagsFromIElementFactory.getInstance(node.type)
            MarkdownTokenTypes.CODE_FENCE_CONTENT,
            MarkdownTokenTypes.CODE_LINE,
            -> codeLineHandler(node)
            MarkdownTokenTypes.TEXT -> textHandler(node, keepAllFormatting)
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

    private fun List<ASTNode>.filterSpacesAndEOL() =
        this.filterNot { it.type == MarkdownTokenTypes.WHITE_SPACE || it.type == MarkdownTokenTypes.EOL }

    private fun List<ASTNode>.evaluateChildren(keepAllFormatting: Boolean = false): List<DocTag> =
        this.removeUselessTokens().swapImagesThatShouldBeLinks(keepAllFormatting).mergeLeafASTNodes().flatMap { visitNode(it, keepAllFormatting) }

    private fun List<ASTNode>.swapImagesThatShouldBeLinks(keepAllFormatting: Boolean): List<ASTNode> =
        if (keepAllFormatting) {
            this
        } else {
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

    private fun ASTNode.isNotLeaf() = this is CompositeASTNode || this.type in notLeafNodes

    private fun List<ASTNode>.isNotLeaf(index: Int): Boolean =
        if (index in 0..this.lastIndex)
            this[index].isNotLeaf()
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
                        children += mergedLeafNode(this, index, startOffset, sIndex)
                        break
                    }
                    index++
                }
                if (index == this.lastIndex) {
                    children += mergedLeafNode(this, index, startOffset, sIndex)
                }
            }
            index++
        }
        return children
    }

    private fun mergedLeafNode(nodes: List<ASTNode>, index: Int, startOffset: Int, sIndex: Int): LeafASTNode {
        val endOffset = nodes[index].endOffset
        val type = if (nodes.subList(sIndex, index)
                .any { it.type == MarkdownTokenTypes.CODE_LINE }
        ) MarkdownTokenTypes.CODE_LINE else MarkdownTokenTypes.TEXT
        return LeafASTNode(type, startOffset, endOffset)
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


    public companion object {
        public fun DRI.fqDeclarationName(): String? {
            if (this.target !is PointingToDeclaration) {
                return null
            }
            return listOfNotNull(this.packageName, this.classNames, this.callable?.name)
                .joinToString(separator = ".")
                .takeIf { it.isNotBlank() }
        }
    }
}

