import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "value" to listOf(1, 2, 3, 3),
        "type" to listOf("a", "b", "a", "b")
    )
    val gb = df.groupBy { expr { "Category: ${type.uppercase()}" } named "category" }
    val categoryKey = gb.keys.category

    val dfWithCategory = gb.concatWithKeys()

    val category: DataColumn<String> = dfWithCategory.category

    return "OK"
}
