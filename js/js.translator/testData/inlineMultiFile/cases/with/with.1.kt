/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/with.1.kt
 */

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
