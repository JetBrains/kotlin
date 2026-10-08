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

fun box(): String {
    // `favoriteColor` is not part of `UserLike`, the schema is still compatible with it
    val df = dataFrameOf(
        "name" to columnOf("Alice", "John"),
        "age" to columnOf(12, 13),
        "favoriteColor" to columnOf("blue", "yellow"),
    )

    // no `cast<UserLike>()`: the generated schema marker of `df` has `UserLike` as a supertype
    val actual = df.nameAndAge()
    val expected = "Alice is 12 years old, John is 13 years old"
    if (actual != expected) return "Expected <$expected>, got <$actual>"

    // the extension properties of the marker keep working
    if (df.favoriteColor[0] != "blue") return "favoriteColor: ${df.favoriteColor[0]}"
    if (df[0].name != "Alice") return "name: ${df[0].name}"

    df.compareSchemas(strict = true)
    return "OK"
}
