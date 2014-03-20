package test.pegdown

import junit.framework.TestCase
import org.pegdown.*
import org.pegdown.LinkRenderer.Rendering
import org.pegdown.ast.*

class PegdownTest() : TestCase() {
    var markdownProcessor = PegDownProcessor(Extensions.ALL)
    var linkRenderer = CustomLinkRenderer()

    fun testPegDown() {
        val markups = listOf(
        "hello **there **",
        "a [[WikiLink]] blah",
        "a [[WikiLink someText]] blah",
        "a [[SomeClass.property]] blah",
        "a [[SomeClass.method()]] blah",
        "a [Link](somewhere) blah")

        for (text in markups) {
            val answer = markdownProcessor.markdownToHtml(text, linkRenderer)!!
            println("$text = $answer")
        }
    }
}


class CustomLinkRenderer : LinkRenderer() {
    override fun render(node: WikiLinkNode?): Rendering? {
        println("LinkRenderer.render(WikiLinkNode): $node")
        return super.render(node)
    }

    override fun render(node: RefLinkNode?, url: String?, title: String?, text: String?): Rendering? {
        println("LinkRenderer.render(RefLinkNode): $node url: $url title: $title text: $text")
        return super.render(node, url, title, text)
    }

    override fun render(node: AutoLinkNode?): Rendering? {
        println("LinkRenderer.render(AutoLinkNode): $node")
        return super.render(node)
    }

    override fun render(node: ExpLinkNode?, text: String?): Rendering? {
        println("LinkRenderer.render(ExpLinkNode): $node text: $text")
        return super.render(node, text)
    }
}
