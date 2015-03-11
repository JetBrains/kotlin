package kotlin.template.html

import kotlin.*
import kotlin.template.*
import kotlin.io.*
import kotlin.util.*
import java.io.*
import java.util.*


trait Factory<T> {
  fun create() : T
}

abstract class Element {
  abstract fun appendTo(builder: StringBuilder): Unit

}

class TextElement(val text : String) : Element() {
  override fun appendTo(builder: StringBuilder) {
    builder.append(text)
  }
}

abstract class Tag(val name : String) : Element() {
  val children = ArrayList<Element>()
  val attributes = HashMap<String, String>()

  protected fun initTag<T : Element>(init : T.()-> Unit) : T
  where default object T : Factory<T> {
    val tag = try {
      T.create()
    } catch (e: NullPointerException) {
      val typeName = javaClass.getName()
      throw UnsupportedOperationException("No default object create() method for $typeName")
    }
    tag.init()
    children.add(tag)
    return tag
  }

  override fun appendTo(builder: StringBuilder): Unit {
    builder.append("<")
    builder.append(name)
    if (!attributes.isEmpty()) {
      for (e in attributes) {
        builder.append(" ${e.key}=\"${e.value}\"")
      }
    }
    if (children.isEmpty()) {
      builder.append("/>")
    } else {
      builder.append(">")
      for (c in children) {
        c.appendTo(builder)
      }
      builder.append("<")
      builder.append(name)
      builder.append(">")
    }

  }

  override fun toString(): String {
    val builder = StringBuilder()
    appendTo(builder)
    return builder.toString()!!
  }
}

abstract class TagWithText(name : String) : Tag(name) {
  fun String.plus() {
    children.add(TextElement(this))
  }
}

class HTML() : TagWithText("html") {
  default object : Factory<HTML> {
    override fun create() = HTML()
  }

  fun head(init : Head.()-> Unit) = initTag(init)

  fun body(init : Body.()-> Unit) = initTag(init)
}

class Head() : TagWithText("head") {
  default object : Factory<Head> {
    override fun create() = Head()
  }

  fun title(init : Title.()-> Unit) = initTag(init)
}

class Title() : TagWithText("title") {
  default object : Factory<Title> {
    override fun create() = Title()
  }
}

abstract class BodyTag(name : String) : TagWithText(name) {
}

class Body() : BodyTag("body") {
  default object : Factory<Body> {
    override fun create() = Body()
  }

  fun b(init : B.()-> Unit) = initTag(init)
  fun p(init : P.()-> Unit) = initTag(init)
  fun h1(init : H1.()-> Unit) = initTag(init)

  fun a(href : String) {
    a(href, {})
  }

  fun a(href : String, init : A.()-> Unit) {
    val a = initTag(init)
    a.href = href
  }
}

class B() : BodyTag("b") {
  default object : Factory<B> {
    override fun create() = B()
  }
}

class P() : BodyTag("p") {
  default object : Factory<P> {
    override fun create() = P()
  }
}
class H1() : BodyTag("h1")   {
  default object : Factory<H1> {
    override fun create() = H1()
  }
}

class A() : BodyTag("a") {
  default object : Factory<A> {
    override fun create() = A()
  }

  var href : String
    get() = attributes["href"] ?: ""
    set(value) {
      attributes["href"] = value
    }
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
abstract class MarkupTemplate() : TemplateSupport() {

  override fun render() {
    val markup = markup()
    print(markup)
  }

  /** Returns the markup to be rendered */
  abstract fun markup(): Iterable<Element>
}

