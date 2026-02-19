import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { isHappy },
        df.select { last() },
        df.select { nameStartsWith("is").last() },
    )

    compareSchemas(
        df.select { name.lastName },
        df.select { name.lastCol() },
    )
    return "OK"
}
