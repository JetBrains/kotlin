package test.pegdown

import kotlin.*
import kotlin.util.*
import kotlin.test.*

import junit.framework.TestCase
import org.pegdown.*
import org.pegdown.ast.*
import org.pegdown.LinkRenderer.Rendering

class PegdownTest() : TestCase() {
    var markdownProcessor = PegDownProcessor(Extensions.ALL)
    var linkRenderer = CustomLinkRenderer()

    fun testPegDown() {
        val markups = arrayList(
        "hello **there **",
        "a [[WikiLink]] blah",
        "a [[WikiLink someText]] blah",
        "a [[SomeClass.property]] blah",
        "a [[SomeClass.method()]] blah",
        "a [Link](somewhere) blah")

        for (text in markups) {
            val answer = markdownProcessor.markdownToHtml(text, linkRenderer).sure()
            println("$text = $answer")
        }
    }
}


class CustomLinkRenderer() : LinkRenderer() {

    public override fun render(node : WikiLinkNode?) : Rendering? {
        println("LinkRenderer.render(WikiLinkNode): $node")
        return super.render(node)
    }

    public override fun render(node : RefLinkNode?, url : String?, title : String?, text : String?) : Rendering? {
        println("LinkRenderer.render(RefLinkNode): $node url: $url title: $title text: $text")
        return super.render(node, url, title, text)
    }

    public override fun render(node : AutoLinkNode?) : Rendering? {
        println("LinkRenderer.render(AutoLinkNode): $node")
        return super.render(node)
    }

    public override fun render(node : ExpLinkNode?, text : String?) : Rendering? {
        println("LinkRenderer.render(ExpLinkNode): $node text: $text")
        return super.render(node, text)
    }
}
