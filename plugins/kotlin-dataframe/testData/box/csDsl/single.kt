import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name },
        df.select { name }.select { single() },
        df.select { nameStartsWith("name").single() },
    )

    compareSchemas(
        df.select { name.firstName },
        df.remove { name.lastName }.select { name.singleCol() },
    )
    return "OK"
}
