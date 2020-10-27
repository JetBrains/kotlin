import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testBlocks() {
    assertTrue(Blocks.blockIsNull(null))
    assertFalse(Blocks.blockIsNull({}))

    assertEquals(null, Blocks.nullBlock)
    assertNotEquals(null, Blocks.notNullBlock)

    assertEquals(10, Blocks.same({ a, b, c, d -> a + b + c + d })!!(1, 2, 3, 4))

    assertEquals(222, callProvidedBlock(object : NSObject(), BlockProviderProtocol {
        override fun block(): (Int) -> Int = { it * 2 }
    }, 111))

    assertEquals(322, callPlusOneBlock(object : NSObject(), BlockConsumerProtocol {
        override fun callBlock(block: ((Int) -> Int)?, argument: Int) = block!!(argument)
    }, 321))
}
