package kdump

import kotlin.test.Test

class StructuralAppendableTest {
  @Test
  fun str() {
//    assertEquals("""dump
//  header: "Foo"
//  endianness: big
//  id size: 2""".trimIndent(), structString { append(Dump("Foo", Endianness.BIG_ENDIAN, IdSize.BITS_16, listOf())) })
  }
}