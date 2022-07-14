import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

@DataSchema
interface Person {
    val age: Int
}

val df = dataFrameOf("age")(1).cast<Person>()

fun insert_properties() {
    df.insert("year of birth") { 2021 - age }.under("test")
}