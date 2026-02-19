import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Person {
    val age: Int
}

fun box(): String {
    val df = dataFrameOf("age")(1).cast<Person>()
    val df1 = df.insert("year of birth") { 2021 - age }.under("test")
    df1.age
    df1.test.`year of birth`
    df1.compareSchemas(strict = true)

    val df2 = dataFrameOf("a")(11)
        .group { a }.into("group")
        .insert("b") { group.a * 111 }.under { group }
    df2.group.a
    df2.group.b
    df2.compareSchemas(strict = true)
    return "OK"
}
