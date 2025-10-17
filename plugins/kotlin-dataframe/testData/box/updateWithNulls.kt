import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    run {
        val df = dataFrameOf(
            "a" to columnOf(1, 2),
            "b" to columnOf("abc", "def"),
        ).update { a and b }.where { index() == 0 }.withNull()
        val a1: DataColumn<Int?> = df.a
        val a2: DataColumn<String?> = df.b
        df.compareSchemas(strict = true)
    }

    run {
        val df = dataFrameOf(
            "a" to columnOf(1, 2),
            "b" to columnOf("abc", "def"),
        ).fillNulls { a and b }.withNull()
        val a1: DataColumn<Int> = df.a
        val a2: DataColumn<String> = df.b
        df.compareSchemas(strict = true)
    }

    run {
        val df = dataFrameOf(
            "a" to columnOf(1, 2),
            "b" to columnOf("abc", "def"),
        ).fillNulls { a and b }.where { index() == 0 }.withNull()
        val a1: DataColumn<Int> = df.a
        val a2: DataColumn<String> = df.b
        df.compareSchemas(strict = true)
    }
    return "OK"
}
