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

fun DataRow<UserLike>.describe(): String = "$name is $age years old"

fun describeAll(rows: List<DataRow<UserLike>>): String = rows.joinToString { it.describe() }

fun box(): String {
    val df = dataFrameOf(
        "name" to columnOf("Alice", "John"),
        "age" to columnOf(12, 13),
        "favoriteColor" to columnOf("blue", "yellow"),
    )

    // `DataRow` is covariant too, so a row of a compatible frame is a `DataRow<UserLike>`
    val row = df[0]
    if (row.describe() != "Alice is 12 years old") return "single row: ${row.describe()}"

    val allRows = df.rows().joinToString { it.describe() }
    if (allRows != "Alice is 12 years old, John is 13 years old") return "rows: $allRows"

    val asArgument = describeAll(df.rows().toList())
    if (asArgument != "Alice is 12 years old, John is 13 years old") return "as argument: $asArgument"

    // a marker produced by a chained refined call is polymorphic as well
    val withGreeting = df.add("greeting") { "hi" }
    if (withGreeting[1].describe() != "John is 13 years old") return "after add: ${withGreeting[1].describe()}"

    return "OK"
}
