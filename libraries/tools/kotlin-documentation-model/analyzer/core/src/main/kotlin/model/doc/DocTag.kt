package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

sealed class DocTag(
    override val children: List<DocTag>,
    val params: Map<String, String>
) : WithChildren<DocTag> {
    override fun equals(other: Any?): Boolean =
        (
                other != null &&
                        other::class == this::class &&
                        this.children == (other as DocTag).children &&
                        this.params == other.params
                )

    override fun hashCode(): Int = children.hashCode() + params.hashCode()
}

class A(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Big(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class B(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class BlockQuote(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)

object Br : DocTag(emptyList(), emptyMap())
class Cite(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
sealed class Code(children: List<DocTag>, params: Map<String, String>) : DocTag(children, params)
class CodeInline(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    Code(children, params)

class CodeBlock(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : Code(children, params)
class Dd(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Dfn(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Dir(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Div(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Dl(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Dt(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Em(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Font(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Footer(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Frame(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class FrameSet(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)

class H1(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class H2(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class H3(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class H4(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class H5(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class H6(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Head(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Header(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Html(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class I(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class IFrame(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Img(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Input(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Li(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Link(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Listing(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Main(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Menu(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Meta(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Nav(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class NoFrames(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)

class NoScript(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)

class Ol(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class P(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Pre(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Script(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Section(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Small(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Span(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Strikethrough(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)

class Strong(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Sub(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Sup(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Table(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Text(val body: String = "", children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params) {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.body == (other as Text).body
    override fun hashCode(): Int = super.hashCode() + body.hashCode()
}

class TBody(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Td(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class TFoot(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Th(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class THead(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Title(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Tr(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Tt(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class U(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Ul(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class Var(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class DocumentationLink(val dri: DRI, children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params) {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.dri == (other as DocumentationLink).dri
    override fun hashCode(): Int = super.hashCode() + dri.hashCode()
}

object HorizontalRule : DocTag(emptyList(), emptyMap())
class Index(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) : DocTag(children, params)
class CustomDocTag(children: List<DocTag> = emptyList(), params: Map<String, String> = emptyMap()) :
    DocTag(children, params)
