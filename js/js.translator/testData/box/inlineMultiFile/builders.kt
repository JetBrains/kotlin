// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1661
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/builders/builders.1.kt
 */


// FILE: foo.kt
package foo

fun testAllInline() : String {
    val args = arrayOf("1", "2", "3")
    val result =
            html {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                body {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testHtmlNoInline() : String {
    val args = arrayOf("1", "2", "3")
    val result =
            htmlNoInline() {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                body {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testBodyNoInline() : String {
    val args = arrayOf("1", "2", "3")
    val result =
            html {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                bodyNoInline {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testBodyHtmlNoInline() : String {
    val args = arrayOf("1", "2", "3")
    val result =
            htmlNoInline {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                bodyNoInline {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "https://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun box(): String {
    var expected = testAllInline();

    if (expected != testHtmlNoInline()) return "fail 1: ${testHtmlNoInline()}\nbut expected\n${expected} "

    if (expected != testBodyNoInline()) return "fail 2: ${testBodyNoInline()}\nbut expected\n${expected} "

    if (expected != testBodyHtmlNoInline()) return "fail 3: ${testBodyHtmlNoInline()}\nbut expected\n${expected} "

    return "OK"
}


// FILE: bar.kt
package foo


abstract class Element {
    abstract fun render(builder: StringBuilder, indent: String)

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

class TextElement(val text: String) : Element() {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

abstract class Tag(val name: String) : Element() {
    val children = ArrayList<Element>()
    val attributes = HashMap<String, String>()

    inline fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, indent + "  ")
        }
        builder.append("$indent</$name>\n")
    }

    private fun renderAttributes(): String? {
        val builder = StringBuilder()
        for (a in attributes.keys) {
            builder.append(" $a=\"${attributes[a]}\"")
        }
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

class HTML() : TagWithText("html") {
    inline fun head(init: Head.() -> Unit) = initTag(Head(), init)

    inline fun body(init: Body.() -> Unit) = initTag(Body(), init)

    fun bodyNoInline(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head() : TagWithText("head") {
    inline fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title() : TagWithText("title")

abstract class BodyTag(name: String) : TagWithText(name) {
    inline fun b(init: B.() -> Unit) = initTag(B(), init)
    inline fun p(init: P.() -> Unit) = initTag(P(), init)
    inline fun pNoInline(init: P.() -> Unit) = initTag(P(), init)
    inline fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    inline fun ul(init: UL.() -> Unit) = initTag(UL(), init)
    inline fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }
}

class Body() : BodyTag("body")
class UL() : BodyTag("ul") {
    inline fun li(init: LI.() -> Unit) = initTag(LI(), init)
}

class B() : BodyTag("b")
class LI() : BodyTag("li")
class P() : BodyTag("p")
class H1() : BodyTag("h1")
class A() : BodyTag("a") {
    public var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }
}

inline fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

fun htmlNoInline(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}
