import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name and age and isHappy },
        df.select { withoutNulls() },
        df.select { all().withoutNulls() },
    )

    compareSchemas(
        dfGroup.select { name.firstName.firstName },
        dfGroup.select { name.firstName.colsWithoutNulls() },
    )

    return "OK"
}
