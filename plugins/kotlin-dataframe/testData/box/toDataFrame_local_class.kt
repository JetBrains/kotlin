import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    data class Name(val firstName: String, val lastName: String)

    data class Score(val subject: String, val value: Int)

    data class Student(val name: Name, val age: Int, val scores: List<Score>)

    val students = listOf(
        Student(Name("Alice", "Cooper"), 15, listOf(Score("math", 4), Score("biology", 3))),
        Student(Name("Bob", "Marley"), 20, listOf(Score("music", 5))),
    ).toDataFrame()

    students.compareSchemas(strict = true)
    return "OK"
}
