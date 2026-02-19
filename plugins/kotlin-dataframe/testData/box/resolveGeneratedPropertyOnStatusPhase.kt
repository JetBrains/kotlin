import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class MySchema(
    @ColumnName("Col")
    val col: Int
)

fun box(): String {
    dataFrameOf("Col" to listOf(1, 2, 3)).cast<MySchema>().col
    return "OK"
}

// triggers property generation before annotation arguments are resolved
private fun DataFrame<MySchema>.convert() = col