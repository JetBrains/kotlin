package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmMemoryApi::class)
class MemoryAccessTestTest {
    @Test
    fun testPointer() {
        val p: Pointer = Pointer(19u)
        assertEquals(p.address, 19u)
        assertEquals((p + 10u).address, 29u)
        assertEquals((p - 10u).address, 9u)
        assertEquals((p + 10).address, 29u)
        assertEquals((p - 10).address, 9u)
    }

    fun <T> testLoadStore(values: List<T>, typeSize: Int, store: (Pointer, T) -> Unit, load: (Pointer) -> T) {
        withScopedMemoryAllocator { a ->
            // Memory layout: [Long1][T][Long2]
            val ptrToLong1 = a.allocate(24)
            val ptrToT = ptrToLong1 + 8
            val ptrToLong2 = ptrToT + typeSize

            for (x in values) {
                val prevLong = ptrToLong1.loadLong()
                val nextLong = ptrToLong2.loadLong()

                store(ptrToT, x)
                assertEquals(load(ptrToT), x)

                assertEquals(ptrToLong1.loadLong(), prevLong)
                assertEquals(ptrToLong2.loadLong(), nextLong)
            }
        }
    }

    @Test
    fun testByte() {
        val bytes = listOf<Byte>(0, Byte.MIN_VALUE, Byte.MAX_VALUE, -91, -21, -47, -72, -42, 118, 120, 125, 21, -43)
        testLoadStore(bytes, 1, { p, v -> p.storeByte(v) }, { it.loadByte() })
    }

    @Test
    fun testShort() {
        val shorts = listOf<Short>(
            0, Short.MIN_VALUE, Short.MAX_VALUE, 26350, 17667, 5437, 1381,
            21183, 26042, -25961, -22913, 9128, -10684
        )
        testLoadStore(shorts, 2, { p, v -> p.storeShort(v) }, { it.loadShort() })
    }

    @Test
    fun testInt() {
        val ints = listOf<Int>(
            0, Int.MIN_VALUE, Int.MAX_VALUE,
            1348618689, -299556943, -394977414, -621300994, 1034622853, -1010496662,
            -2102993550, 199131417, 407819728, -1093382545
        )
        testLoadStore(ints, 4, { p, v -> p.storeInt(v) }, { it.loadInt() })
    }

    @Test
    fun testLong() {
        val longs = listOf<Long>(
            0, Long.MIN_VALUE, Long.MAX_VALUE,
            6964777768087685094, -3965399897925814666, 6876207943944046195, 7675081221595661767,
            -3388229176969119769, 4265730675328983821, -4893379785828386453, -7516879919690485136,
            8512965883914804069, 6155050932825287650
        )
        testLoadStore(longs, 8, { p, v -> p.storeLong(v) }, { it.loadLong() })
    }

    @Test
    fun testAccessingWithDifferentTypes() {
        withScopedMemoryAllocator { a ->
            val size = 16
            val sizeU = size.toUInt()
            val pointer = a.allocate(size)
            val addr = pointer.address

            fun fillWith(value: Byte) {
                for (ptr in addr..<addr + sizeU) {
                    Pointer(ptr).storeByte(value)
                }
            }

            fun fillWith(value: Short) {
                for (ptr in addr..<addr + sizeU step 2) {
                    Pointer(ptr).storeShort(value)
                }
            }

            fun fillWith(value: Int) {
                for (ptr in addr..<addr + sizeU step 4) {
                    Pointer(ptr).storeInt(value)
                }
            }

            fun fillWith(value: Long) {
                for (ptr in addr..<addr + sizeU step 8) {
                    Pointer(ptr).storeLong(value)
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