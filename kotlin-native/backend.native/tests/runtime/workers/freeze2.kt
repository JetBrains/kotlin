/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze2

import kotlin.test.*

import kotlin.native.concurrent.*

data class Data(var int: Int)

@Test fun runTest() {
    // Ensure that we can not mutate frozen objects and arrays.
    val a0 = Data(2)
    a0.int++
    a0.freeze()
    assertFailsWith<InvalidMutabilityException> {a0.int++ }

    val a1 = ByteArray(2)
    a1[1]++
    a1.freeze()
    assertFailsWith<InvalidMutabilityException> { a1[1]++ }

    val a2 = ShortArray(2)
    a2[1]++
    a2.freeze()
    assertFailsWith<InvalidMutabilityException> { a2[1]++ }

    val a3 = IntArray(2)
    a3[1]++
    a3.freeze()
    assertFailsWith<InvalidMutabilityException> { a3[1]++ }

    val a4 = LongArray(2)
    a4[1]++
    a4.freeze()
    assertFailsWith<InvalidMutabilityException> { a4[1]++ }

    val a5 = BooleanArray(2)
    a5[1] = true
    a5.freeze()
    assertFailsWith<InvalidMutabilityException> { a5[1] = false }

    val a6 = CharArray(2)
    a6[1] = 'a'
    a6.freeze()
    assertFailsWith<InvalidMutabilityException> { a6[1] = 'b' }

    val a7 = FloatArray(2)
    a7[1] = 1.0f
    a7.freeze()
    assertFailsWith<InvalidMutabilityException> { a7[1] = 2.0f }

    val a8 = DoubleArray(2)
    a8[1] = 1.0
    a8.freeze()
    assertFailsWith<InvalidMutabilityException> { a8[1] = 2.0 }

    // Ensure that String and integral boxes are frozen by default, by passing local to the worker.
    val worker = Worker.start()
    var data: Any = "Hello" + " " + "world"
    assert(data.isFrozen)
    worker.execute(TransferMode.SAFE, { data } ) {
        input -> println("Worker 1: $input")
    }.result

    data = 42
    assert(data.isFrozen)
    worker.execute(TransferMode.SAFE, { data } ) {
        input -> println("Worker2: $input")
    }.result

    data = 239.0
    assert(data.isFrozen)
    worker.execute(TransferMode.SAFE, { data } ) {
        input -> println("Worker3: $input")
    }.result

    data = 'a'
    assert(data.isFrozen)
    worker.execute(TransferMode.SAFE, { data } ) {
        input -> println("Worker4: $input")
    }.result

    worker.requestTermination().result

    println("OK")
}