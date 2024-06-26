import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class Tests {
    val text = """[{"a":null, "b":1},{"a":null, "b":2}]"""

    fun test1() {
        val df = DataFrame.readJsonStr(text)
        df.a
        df.b
    }
}

fun box(): String {
    Tests().test1()
    return "OK"
}
