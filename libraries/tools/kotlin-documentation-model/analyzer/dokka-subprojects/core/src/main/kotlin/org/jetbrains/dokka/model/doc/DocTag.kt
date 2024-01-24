/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

public sealed class DocTag : WithChildren<DocTag> {
    public abstract val params: Map<String, String>

    public companion object {
        public fun contentTypeParam(type: String): Map<String, String> = mapOf("content-type" to type)
    }
}

public data class A(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Big(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class B(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class BlockQuote(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public object Br : DocTag() {
    override val children: List<DocTag> = emptyList()
    override val params: Map<String, String> = emptyMap()
}

public data class Cite(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public sealed class Code : DocTag()

public data class CodeInline(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : Code()

public data class CodeBlock(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : Code()

public data class CustomDocTag(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap(),
    val name: String
) : DocTag()

public data class Dd(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Dfn(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Dir(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Div(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Dl(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class DocumentationLink(
    val dri: DRI,
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Dt(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Em(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Font(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Footer(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Frame(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class FrameSet(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H1(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H2(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H3(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H4(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H5(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class H6(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Head(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Header(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public object HorizontalRule : DocTag() {
    override val children: List<DocTag> = emptyList()
    override val params: Map<String, String> = emptyMap()
}

public data class Html(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class I(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class IFrame(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Img(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Index(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Input(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Li(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Link(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Listing(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Main(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Menu(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Meta(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Nav(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class NoFrames(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class NoScript(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Ol(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class P(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Pre(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Script(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Section(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Small(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Span(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Strikethrough(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Strong(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Sub(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Sup(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Table(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Text(
    val body: String = "",
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class TBody(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Td(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class TFoot(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Th(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class THead(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Title(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Tr(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Tt(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class U(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Ul(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Var(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

public data class Caption(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

