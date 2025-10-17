import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("a" to columnOf(1, null, 3)).convert { a }.notNull { it.toString() }.let { df ->
        val col: DataColumn<String?> = df.a
        df.compareSchemas(strict = true)
    }

    dataFrameOf("a" to columnOf(1, 2, 3)).convert { a }.notNull { it.toString() }.let { df ->
        val col: DataColumn<String> = df.a
        df.compareSchemas(strict = true)
    }

    dataFrameOf("group" to columnOf("a" to columnOf("1", "2"))).convert { group }.notNull { "test" }.let { df ->
        val col: DataColumn<String> = df.group
    }

    return "OK"
}
