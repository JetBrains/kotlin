package runtime.workers.mutableData1

import kotlin.native.concurrent.*
import kotlin.test.*

// See https://youtrack.jetbrains.com/issue/KT-39145
@Test
fun kt39145_1() = withWorker {
    execute(TransferMode.SAFE, { "test" }) {
        val data = MutableData(1_000)
        val bytes = byteArrayOf(0, 10, 20, 30)
        data.append(bytes, 0, 4)
        assertEquals(4, data.size)

        assertContentsEquals(bytes, data)
    }.result
}

@Test
fun kt39145_2() = withWorker {
    val externalData = MutableData(1_000)
    execute(TransferMode.SAFE, { externalData }) {
        val bytes = byteArrayOf(0, 10, 20, 30)
        it.append(bytes, 0, 4)
        assertEquals(4, it.size)

        assertContentsEquals(bytes, it)
    }.result
}

@Test
fun kt39145_3() {
    val mainThreadData = MutableData(1_000)
    val bytes = byteArrayOf(0, 10, 20, 30)
    mainThreadData.append(bytes, 0, 4)
    assertEquals(4, mainThreadData.size)

    assertContentsEquals(bytes, mainThreadData)
}

private fun assertContentsEquals(expected: ByteArray, actual: MutableData) {
    assertEquals(expected.size, actual.size)

    expected.forEachIndexed { index, byte ->
        assertEquals(byte, actual.get(index))
    }
}