import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Person(
    val name: Name,
) {

    @DataSchema
    data class Name(
        val firstName: String,
        val lastName: String,
    )
}

fun test() {
    val df = dataFrameOf(Person(Person.Name("Alice", "Cooper")))

    df.name.firstName
}
