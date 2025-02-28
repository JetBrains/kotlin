import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { age },
        df.select { nameContains("age") },
        df.select { nameContains("AGE", ignoreCase = true) },
        df.select { all().nameContains("age") },
    )

    compareSchemas(
        df.select { name.firstName },
        df.select { name.colsNameContains("first") },
        df.select { name.colsNameContains("FIRST", ignoreCase = true) },
    )

    compareSchemas(
        df.select { age },
        df.select { nameStartsWith("age") },
        df.select { nameStartsWith("AGE", ignoreCase = true) },
        df.select { all().nameStartsWith("age") },
    )

    compareSchemas(
        df.select { name.firstName },
        df.select { name.colsNameStartsWith("first") },
        df.select { name.colsNameStartsWith("FIRST", ignoreCase = true) },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.colsNameEndsWith("Name") },
        df.select { name.colsNameEndsWith("NAME", ignoreCase = true) },
    )
    return "OK"
}
