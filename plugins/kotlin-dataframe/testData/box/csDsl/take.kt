import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name.firstName },
        df.select { name.takeCols(1) },
    )

    compareSchemas(
        df.select { name.lastName },
        df.select { name.takeLastCols(1) },
    )

    compareSchemas(
        df.select { name and age and city },
        df.select { take(3) },
        df.select { all().take(3) },
    )

    compareSchemas(
        df.select { city and weight and isHappy },
        df.select { all().takeLast(3) },
        df.select { takeLast(3) },
    )
    return "OK"
}
