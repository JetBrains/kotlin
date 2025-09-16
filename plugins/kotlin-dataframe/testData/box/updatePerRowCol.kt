import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val input = dataFrameOf(
        "a" to columnOf("1", "2", "3"),
        "b" to columnOf("4", null, "6"),
    )
    // perRowCol signature gives us not enough information to know that computed value is non-null String
    input.update { a and b }.perRowCol { _, col -> "newValue for ${col.name()}" }.let { df ->
        val col: DataColumn<String?> = df.a
        val col1: DataColumn<String?> = df.b
        df.assert()
    }
    return "OK"
}
