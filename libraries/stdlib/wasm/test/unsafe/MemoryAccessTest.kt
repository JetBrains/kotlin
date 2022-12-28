package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmMemoryApi::class)
class MemoryAccessTestTest {
    @Test
    fun testScopedAllocator() {
        withScopedMemoryAllocator { a ->
            val size = 16
            val pointer = a.allocate(size)
            val addr = pointer.toInt()

            fun fillWith(value: Byte) {
                for (ptr in addr..<addr + size) {
                    ptr.toPointer().storeByte(value)
                }
            }

            fun fillWith(value: Short) {
                for (ptr in addr..<addr + size step 2) {
                    ptr.toPointer().storeShort(value)
                }
            }

            fun fillWith(value: Int) {
                for (ptr in addr..<addr + size step 4) {
                    ptr.toPointer().storeInt(value)
                }
            }

            fun fillWith(value: Long) {
                for (ptr in addr..<addr + size step 8) {
                    ptr.toPointer().storeLong(value)
                }
            }

            fun checkMem(
                bytes: List<Byte>,
                shorts: List<Short>,
                ints: List<Int>,
                longs: List<Long>
            ) {
                assertEquals(bytes.size, size)
                assertEquals(shorts.size, size / 2)
                assertEquals(ints.size, size / 4)
                assertEquals(longs.size, size / 8)
                for (i in 0..<size) {
                    assertEquals((pointer + i).loadByte(), bytes[i])
                }
                for (i in 0..<size / 2) {
                    assertEquals((pointer + i * 2).loadShort(), shorts[i])
                }
                for (i in 0..<size / 4) {
                    assertEquals((pointer + i * 4).loadInt(), ints[i])
                }
                for (i in 0..<size / 8) {
                    assertEquals((pointer + i * 8).loadLong(), longs[i])
                }
            }

            fun checkZero() {
                checkMem(
                    bytes = List(size) { 0 },
                    shorts = List(size / 2) { 0 },
                    ints = List(size / 4) { 0 },
                    longs = List(size / 8) { 0L }
                )
            }

            fillWith(0.toByte())
            checkZero()
            fillWith(0x0F.toByte())
            checkMem(
                bytes = List(size) { 0x0F },
                shorts = List(size / 2) { 0x0F0F },
                ints = List(size / 4) { 0x0F0F0F0F },
                longs = List(size / 8) { 0x0F0F0F0F0F0F0F0FL }
            )

            fillWith(0.toShort())
            checkZero()
            fillWith(0xABCDu.toShort())
            checkMem(
                bytes = mutableListOf<Byte>().also { list ->
                    repeat(size / 2) {
                        // little-endian
                        list += 0xCD.toByte()
                        list += 0xAB.toByte()
                    }
                },
                shorts = List(size / 2) { 0xABCDu.toShort() },
                ints = List(size / 4) { 0xABCDABCDu.toInt() },
                longs = List(size / 8) { 0xABCDABCDABCDABCDuL.toLong() }
            )
            fillWith(0.toInt())
            checkZero()
            fillWith(0xFFFFFFFFu.toInt())
            checkMem(
                bytes = List(size) { 0xFFu.toByte() },
                shorts = List(size / 2) { 0xFFFFu.toShort() },
                ints = List(size / 4) { 0xFFFFFFFFu.toInt() },
                longs = List(size / 8) { 0xFFFFFFFFFFFFFFFFuL.toLong() }
            )
            fillWith(0L)
            checkZero()
            fillWith(0x1212121212121212L)
            checkMem(
                bytes = List(size) { 0x012 },
                shorts = List(size / 2) { 0x1212 },
                ints = List(size / 4) { 0x12121212 },
                longs = List(size / 8) { 0x1212121212121212L }
            )
        }
    }
}