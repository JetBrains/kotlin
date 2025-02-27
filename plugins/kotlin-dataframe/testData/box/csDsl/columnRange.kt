import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { age and city and weight },
        df.select { age..weight },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.firstName..name.lastName }
    )
    return "OK"
}
