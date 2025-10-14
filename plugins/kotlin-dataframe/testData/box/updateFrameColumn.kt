import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.math.max

fun box(): String {
    val size = 4

    val data = dataFrameOf(
        "a" to List(size) { it.toString() },
        "b" to List(size) {
            dataFrameOf("c" to columnOf(2)).duplicateRows(max(1, it))
        }
    )

    // compileTimeSchema is not accurate for DataColumn<DataFrame<>?>

    data.update { b }.with { df -> df.takeIf { df.rowsCount() % 2 == 0 } }.let { df ->
        val col: DataColumn<Int>? = df[0].b?.c
    }

    data.update { b }.where { it.rowsCount() / 2 == 0 }.with { null }.let { df ->
        val col: DataColumn<Int>? = df[0].b?.c
    }

    val filler = data.b[0]
    data
        .update { b }.where { it.rowsCount() / 2 == 0 }.with { null }
        .fillNulls { b }.with { filler }
        .let { df ->
            val col: DataColumn<Int> = df[0].b.c
            df.compareSchemas(strict = true)
        }

    return "OK"
}
