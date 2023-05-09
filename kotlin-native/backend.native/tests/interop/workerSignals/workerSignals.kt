@file:OptIn(kotlin.ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)

import kotlin.native.concurrent.*
import kotlin.test.*
import workerSignals.*

const val defaultValue = 0
const val newValue = 42

fun main() {
    setupSignalHandler()

    withWorker {
        val before = execute(TransferMode.SAFE, {}) {
            getValue()
        }.result
        assertEquals(defaultValue, getValue())
        assertEquals(defaultValue, before)

        signalThread(platformThreadId, newValue)
        val after = execute(TransferMode.SAFE, {}) {
            getValue()
        }.result
        assertEquals(defaultValue, getValue())
        assertEquals(newValue, after)
    }
}
