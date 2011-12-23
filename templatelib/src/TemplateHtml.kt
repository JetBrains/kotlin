namespace std.template.html

import std.*
import std.template.*
import std.io.*
import std.util.*
import java.io.*
import java.util.*


trait Factory<T> {
  fun create() : T
}

abstract class Element

class TextElement(val text : String) : Element

abstract class Tag(val name : String) : Element {
  val children = ArrayList<Element>()
  val attributes = HashMap<String, String>()

  protected fun initTag<T : Element>(init : T.()-> Unit) : T
  where class object T : Factory<T> {
    val tag = T.create()
    tag.init()
    children.add(tag)
    return tag
  }
}

abstract class TagWithText(name : String) : Tag(name) {
  fun String.plus() {
    children.add(TextElement(this))
  }
}

class HTML() : TagWithText("html") {
  class object : Factory<HTML> {
    override fun create() = HTML()
  }

  fun head(init : Head.()-> Unit) = initTag(init)

  fun body(init : Body.()-> Unit) = initTag(init)
}

class Head() : TagWithText("head") {
  class object : Factory<Head> {
    override fun create() = Head()
  }

  fun title(init : Title.()-> Unit) = initTag(init)
}

class Title() : TagWithText("title")

abstract class BodyTag(name : String) : TagWithText(name) {
}

class Body() : BodyTag("body") {
  class object : Factory<Body> {
    override fun create() = Body()
  }

  fun b(init : B.()-> Unit) = initTag(init)
  fun p(init : P.()-> Unit) = initTag(init)
  fun h1(init : H1.()-> Unit) = initTag(init)

  fun a(href : String) {
    a(href) {}
  }

  fun a(href : String, init : A.()-> Unit) {
    val a = initTag(init)
    a.href = href
  }
}

class B() : BodyTag("b")
class P() : BodyTag("p")
class H1() : BodyTag("h1")

class A() : BodyTag("a") {
  var href: String? = null
  /*
    var href : String
      get() = attributes["href"]
      set(value) { attributes["href"] = value }
  */
}

fun body(init: Body.()-> Unit): Body {
  val elem = Body()
  elem.init()
  return elem
}

fun html(init : HTML.()-> Unit) : HTML {
  val html = HTML()
  html.init()
  return html
}

/**
 * Base class for templates which generate markup (XML or HTML for example)
 * which have additional helper methods for escaping markup etc
 */
abstract class MarkupTemplate() : TemplateSupport {

  override fun render() {
    val markup = markup()
    print(markup)
  }

  /** Returns the markup to be rendered */
  abstract fun markup(): Iterable<Element>
}

