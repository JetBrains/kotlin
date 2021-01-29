package com.bnorm.power

import org.junit.Test
import kotlin.test.assertEquals

class RegexMatchTest {
  @Test
  fun `regex matches`() {
    val actual = executeMainAssertion("""assert("Hello, World".matches("[A-Za-z]+".toRegex()))""")
    assertEquals(
      """
      Assertion failed
      assert("Hello, World".matches("[A-Za-z]+".toRegex()))
                            |                   |
                            |                   [A-Za-z]+
                            false
      """.trimIndent(),
      actual
    )
  }

  @Test
  fun `infix regex matches`() {
    val actual = executeMainAssertion("""assert("Hello, World" matches "[A-Za-z]+".toRegex())""")
    assertEquals(
      """
      Assertion failed
      assert("Hello, World" matches "[A-Za-z]+".toRegex())
                            |                   |
                            |                   [A-Za-z]+
                            false
      """.trimIndent(),
      actual
    )
  }
}
