import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class KotlinRecord(
    val nullableNumber: Int?,
)

fun box(): String {
    val res = listOf(
        KotlinRecord(
            null,
        )
    ).toDataFrame(maxDepth = 2)
    res.compareSchemas(strict = true)
    return "OK"
}
