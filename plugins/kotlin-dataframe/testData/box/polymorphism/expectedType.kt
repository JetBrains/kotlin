// POLYMORPHIC_DATA_SCHEMAS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface UserLike {
    val name: String
    val age: Int
}

fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "${it.name} is ${it.age} years old" }

fun produce(): DataFrame<UserLike> {
    return dataFrameOf(
        "name" to columnOf("Alice"),
        "age" to columnOf(12),
    )
}

fun produceExpressionBody(): DataFrame<UserLike> = dataFrameOf(
    "name" to columnOf("John"),
    "age" to columnOf(13),
    "favoriteColor" to columnOf("yellow"),
)

fun box(): String {
    // the call is completed against the expected type, the compatible schema is still added to its marker
    val df: DataFrame<UserLike> = dataFrameOf(
        "name" to columnOf("Alice", "John"),
        "age" to columnOf(12, 13),
    )
    val expected = "Alice is 12 years old, John is 13 years old"
    if (df.nameAndAge() != expected) return "Expected <$expected>, got <${df.nameAndAge()}>"

    if (produce().nameAndAge() != "Alice is 12 years old") return "produce: ${produce().nameAndAge()}"
    if (produceExpressionBody().nameAndAge() != "John is 13 years old") {
        return "produceExpressionBody: ${produceExpressionBody().nameAndAge()}"
    }

    val frames: List<DataFrame<UserLike>> = listOf(df, produce(), produceExpressionBody())
    if (frames.sumOf { it.rowsCount() } != 4) return "rows: ${frames.sumOf { it.rowsCount() }}"
    return "OK"
}
