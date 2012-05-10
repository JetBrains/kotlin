package test.kotlin.template

import kotlin.*
import org.jetbrains.kotlin.template.*
import kotlin.io.*
import kotlin.util.*
import java.util.*

import junit.framework.TestCase
import junit.framework.Assert.*

class EmailTemplate(var name: String = "James", var time: Date = Date()) : TextTemplate() {
    public override fun render() {
    print("Hello there $name and how are you? Today is $time. Kotlin rocks")
  }
}

class MoreDryTemplate(var name: String = "James", var time: Date = Date()) : TextTemplate() {
    public override fun render() {
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