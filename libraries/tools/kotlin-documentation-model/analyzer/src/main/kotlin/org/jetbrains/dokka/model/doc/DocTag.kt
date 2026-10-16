/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

internal sealed class DocTag : WithChildren<DocTag> {
    public abstract val params: Map<String, String>

    public companion object {
        public fun contentTypeParam(type: String): Map<String, String> = mapOf("content-type" to type)
    }
}

internal data class A(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Big(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class B(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class BlockQuote(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal object Br : DocTag() {
    override val children: List<DocTag> = emptyList()
    override val params: Map<String, String> = emptyMap()
}

internal data class Cite(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal sealed class Code : DocTag()

internal data class CodeInline(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : Code()

internal data class CodeBlock(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : Code()

internal data class CustomDocTag(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap(),
    val name: String
) : DocTag()

internal data class Dd(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Dfn(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Dir(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Div(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Dl(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class DocumentationLink(
    val dri: DRI,
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Dt(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Em(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Font(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Footer(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Frame(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class FrameSet(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H1(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H2(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H3(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H4(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H5(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class H6(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Head(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Header(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal object HorizontalRule : DocTag() {
    override val children: List<DocTag> = emptyList()
    override val params: Map<String, String> = emptyMap()
}

internal data class Html(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class I(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class IFrame(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Img(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Index(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Input(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Li(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Link(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Listing(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Main(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Mark(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Menu(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Meta(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Nav(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class NoFrames(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class NoScript(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Ol(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class P(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Pre(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Script(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Section(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Small(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Span(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Strikethrough(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Strong(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Sub(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Sup(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Table(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Text(
    val body: String = "",
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class TBody(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Td(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class TFoot(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Th(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class THead(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Title(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Tr(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Tt(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class U(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Ul(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Var(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

internal data class Caption(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()
