import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { cols(name, age, city, weight, isHappy) },
        df.select { all() },
    )

    return "OK"
}
