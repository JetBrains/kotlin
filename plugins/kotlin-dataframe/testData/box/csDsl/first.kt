import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name },
        df.select { first() },
        df.select { nameStartsWith("name").first() },
    )

    compareSchemas(
        df.select { name.firstName },
        df.select { name.firstCol() },
    )
    return "OK"
}
