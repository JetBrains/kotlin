/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/use.1.kt
 */

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
