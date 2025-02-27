import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { age and city and weight and isHappy },
        df.select { all().valueCols() },
        df.select { valueCols() },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.valueCols() },
    )

    compareSchemas(
        df.select { age and weight },
        df.select { nameContains("e").valueCols() },
    )

    return "OK"
}
