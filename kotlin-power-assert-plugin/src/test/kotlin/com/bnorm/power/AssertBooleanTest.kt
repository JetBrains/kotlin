package com.bnorm.power

import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class AssertBooleanTest {
  @Test
  fun `test assertTrue transformation`() {
    assertMessage(
      """
      import kotlin.test.assertTrue
      
      fun main() {
        assertTrue(1 != 1)
      }""",
      """
      Assertion failed
      assertTrue(1 != 1)
                   |
                   false
      """.trimIndent(),
      PowerAssertComponentRegistrar(setOf(FqName("kotlin.test.assertTrue")))
    )
  }

  @Test
  fun `test assertFalse transformation`() {
    assertMessage(
      """
      import kotlin.test.assertFalse
      
      fun main() {
        assertFalse(1 == 1)
      }""",
      """
      Assertion failed
      assertFalse(1 == 1)
                    |
                    true
      """.trimIndent(),
      PowerAssertComponentRegistrar(setOf(FqName("kotlin.test.assertFalse")))
    )
  }
}
