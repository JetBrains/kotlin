import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface A {
    val a: Int
}

@DataSchema
interface AB : A {
    val b: String
}

fun box(): String {
    val df = dataFrameOf("a", "b")(1, "2").cast<AB>()
    val res = df.add("c") { 1 }
    val col: DataColumn<Int> = res.a
    res.compareSchemas(strict = true)
    return "OK"
}