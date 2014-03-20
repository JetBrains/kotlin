package test.kotlin.template

import java.util.*
import junit.framework.Assert.*
import junit.framework.TestCase
import org.jetbrains.kotlin.template.*

class EmailTemplate(var name: String = "James", var time: Date = Date()) : TextTemplate() {
    override fun render() {
    print("Hello there $name and how are you? Today is $time. Kotlin rocks")
  }
}

class MoreDryTemplate(var name: String = "James", var time: Date = Date()) : TextTemplate() {
    override fun render() {
    +"Hey there $name and how are you? Today is $time. Kotlin rocks"
  }
}

class TemplateCoreTest() : TestCase() {
  fun testDefaultValues() {
    val text = EmailTemplate().renderToText()
    assertTrue(
      text.startsWith("Hello there James")
    )
  }

  fun testDifferentValues() {
    val text = EmailTemplate("Andrey").renderToText()
    assertTrue(
      text.startsWith("Hello there Andrey")
    )
  }

  fun testMoreDryTemplate() {
    val text = MoreDryTemplate("Alex").renderToText()
    assertTrue(
      text.startsWith("Hey there Alex")
    )
  }
}