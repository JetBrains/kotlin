// EXPECTED_REACHABLE_NODES: 512
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/use.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun Data.test1(d: Data) : Int  {
    val input2 = Input(this)
    val input = Input(this)
    return input.use<Input, Int>{
        val output = Output(d)
        output.use<Output,Int>{
            input.copyTo(output, 10)
        }
    }
}


fun box(): String {

    val result = Data().test1(Data())
    if (result != 100) return "test1: ${result}"

    return "OK"
}


// FILE: test.kt
package test

public class Data()

public data class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}

public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public interface Closeable {
    open public fun close() {}
}

public fun Input.copyTo(output: Output, size: Int): Int {
    return output.doOutput(this.data())
}

public inline fun <T: Closeable, R> T.use(block: (T)-> R) : R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {

        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}