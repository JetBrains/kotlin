// EXPECTED_REACHABLE_NODES: 510
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/with.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun Data.test1(d: Data) : Int  {
    val input = Input(this)
    var result = 10
    with(input) {
         result = use<Int>{
            val output = Output(d)
             use<Int>{
                data()
                copyTo(output, 10)
            }
        }
    }
    return result
}

fun Data.test2(d: Data) : Int  {
    val input = Input(this)
    var result = 10
    with2(input) {
        result = use<Int>{
            val output = Output(d)
            useNoInline<Int>{
                data()
                copyTo(output, 10)
            }
        }
    }
    return result
}

fun box(): String {

    val result = Data().test1(Data())
    if (result != 100) return "test1: ${result}"

    val result2 = Data().test2(Data())
    if (result2 != 100) return "test2: ${result2}"

    return "OK"
}


// FILE: test.kt
package test


public class Data()

public class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}
public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public interface Closeable {
    open public fun close() {}
}

public inline fun <R> use(block: ()-> R) : R {
    return block()
}

public fun <R> useNoInline(block: ()-> R) : R {
    return block()
}


public fun Input.copyTo(output: Output, size: Int): Int {
    return output.doOutput(this.data())
}

public inline fun <T> with2(receiver : T, crossinline body :  T.() -> Unit) : Unit = {receiver.body()}()
