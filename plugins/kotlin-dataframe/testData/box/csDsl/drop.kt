import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name.lastName },
        df.select { name.dropCols(1) },
    )

    compareSchemas(
        df.select { name.firstName },
        df.select { name.dropLastCols(1) },
    )

    compareSchemas(
        df.select { weight and isHappy },
        df.select { drop(3) },
    )

    compareSchemas(
        df.select { name and age },
        df.select { dropLast(3) },
    )
    return "OK"
}
