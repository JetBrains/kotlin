import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Nested(val d: Double)

data class Record(val a: String, val b: Int, val nested: Nested)

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3)

    df.groupBy { a }
        .updateGroups { remove { a } }
        .aggregate { c into "c" }
    return "OK"
}


