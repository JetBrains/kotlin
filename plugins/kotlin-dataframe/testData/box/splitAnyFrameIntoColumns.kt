import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val typed = dataFrameOf("name", "age", "city", "weight")(
        "Alice", 15, "London", 54,
        "Bob", 45, "Dubai", 87,
        "Charlie", 20, "Moscow", null,
        "Charlie", 40, "Milan", null,
        "Bob", 30, "Tokyo", 68,
        "Alice", 20, null, 55,
        "Charlie", 30, "Moscow", 90,
    )
    val df = typed
        .groupBy { name }
        .add("columnGroup") {
            dataFrameOf("a" to columnOf(123)).first()
        }
        .add("nullableColumnGroup") {
            if (index() % 2 == 0) dataFrameOf("a" to columnOf(123)).first() else null
        }
        .add("frameColumn") {
            dataFrameOf("a" to columnOf(123))
        }
        .into("group")
        .split { group }.intoColumns()
    df.assert()
    return "OK"
}