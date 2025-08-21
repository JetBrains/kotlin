import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class A(val str: String, val i: Int)

data class Person(val firstName: String, val lastName: String, val age: Int, val city: String?)

data class Group(val id: String, val participants: List<Person>)

fun box(): String {
    val df = dataFrameOf(
        "col" to listOf(A("123", 321))
    )

    val res = df.unfold { col }
    val str: String = res.col.str[0]
    val i: Int = res.col.i[0]


    val df1 = dataFrameOf(
        "col" to listOf(
            Group("1", listOf(
                Person("Alice", "Cooper", 15, "London"),
                Person("Bob", "Dylan", 45, "Dubai")
            )),
            Group("2", listOf(
                Person("Charlie", "Daniels", 20, "Moscow"),
                Person("Charlie", "Chaplin", 40, "Milan"),
            )),
        )
    )

    val res1: DataColumn<List<Person>> = df1.unfold { col }.col.participants

    val res2: DataColumn<DataFrame<*>> = df1.unfold(maxDepth = 2) { col }.col.participants

    val res3: DataColumn<String> = df1.unfold(maxDepth = 2) { col }.col.participants[0].firstName

    val df2 = dataFrameOf(
        "int" to listOf(1, 2, 3, 4)
    )

    val res4: DataColumn<Int> = df2.unfold { int }.int
    return "OK"
}
