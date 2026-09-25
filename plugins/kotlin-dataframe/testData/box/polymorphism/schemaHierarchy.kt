// POLYMORPHIC_DATA_SCHEMAS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Named {
    val name: String
}

@DataSchema
interface UserLike : Named {
    val age: Int
}

@DataSchema
interface Colored {
    val favoriteColor: String
}

@DataSchema
interface Unrelated {
    val id: Int
}

fun DataFrame<Named>.names(): String = rows().joinToString { it.name }

fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "${it.name} is ${it.age} years old" }

fun DataFrame<Colored>.colors(): String = rows().joinToString { it.favoriteColor }

fun DataFrame<Unrelated>.ids(): String = rows().joinToString { it.id.toString() }

fun box(): String {
    val df = dataFrameOf(
        "name" to columnOf("Alice", "John"),
        "age" to columnOf(12, 13),
        "favoriteColor" to columnOf("blue", "yellow"),
    )

    // several unrelated schemas match at once
    if (df.nameAndAge() != "Alice is 12 years old, John is 13 years old") return "nameAndAge: ${df.nameAndAge()}"
    if (df.colors() != "blue, yellow") return "colors: ${df.colors()}"

    // `Named` is not added to the marker directly, it comes with `UserLike`
    if (df.names() != "Alice, John") return "names: ${df.names()}"

    // `Unrelated` does not match `df`, adding the missing column makes the result match it as well
    val withId = df.add("id") { 0 }
    if (withId.ids() != "0, 0") return "ids: ${withId.ids()}"
    if (withId.nameAndAge() != "Alice is 12 years old, John is 13 years old") return "after add: ${withId.nameAndAge()}"

    return "OK"
}
