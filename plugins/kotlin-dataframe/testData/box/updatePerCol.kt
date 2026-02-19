import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val input = dataFrameOf(
        "a" to columnOf("1", "2", "3"),
        "b" to columnOf("4", null, "6"),
    )
    // perCol signature gives us not enough information to know that computed value is non-null String

    input.update { a and b }.perCol { "newValue for ${it.name()}" }.let { df ->
        val col: DataColumn<String?> = df.a
        val col1: DataColumn<String?> = df.b
        df.assert()
    }

    input.update { a and b }.perCol(mapOf("a" to "newValue for a", "b" to "newValue for b")).let { df ->
        val col: DataColumn<String?> = df.a
        val col1: DataColumn<String?> = df.b
        df.assert()
    }

    val row = dataFrameOf("a" to columnOf("new value for a"), "b" to columnOf("new value for b")).first()
    input.update { a and b }.perCol(row).let { df ->
        val col: DataColumn<String?> = df.a
        val col1: DataColumn<String?> = df.b
        df.assert()
    }
    return "OK"
}
