// DUMP_SCHEMAS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Person(val firstName: String, val lastName: String, val age: Int, val city: String?)

@DataSchema
data class Group(val id: String, val participants: List<Person>)

fun test() {
    val <!SCHEMA!>df<!> = listOf(
        Group("1", listOf(
            Person("Alice", "Cooper", 15, "London"),
            Person("Bob", "Dylan", 45, "Dubai")
        )),
        Group("2", listOf(
            Person("Charlie", "Daniels", 20, "Moscow"),
            Person("Charlie", "Chaplin", 40, "Milan"),
        )),
    ).<!SCHEMA!>toDataFrame<!>(maxDepth = 2)

    // For operator get call schema is reported for the whole expression, so on df two schemas are reported
    <!SCHEMA!><!SCHEMA!>df<!>.participants[0]<!>

    <!SCHEMA!>df<!>.participants.<!SCHEMA!>first<!>()

    <!SCHEMA!>df<!>.<!SCHEMA!>explode<!> { participants }.<!SCHEMA!>participants<!>

    <!SCHEMA!><!SCHEMA!>df<!>.participants[0]<!>.age

    // DataRow
    <!SCHEMA!><!SCHEMA!>df<!>[0]<!>.<!SCHEMA!>participants<!>.age

    // GroupBy
    <!SCHEMA!>df<!>.<!SCHEMA!>explode<!> { participants }.<!SCHEMA!>groupBy<!> { <!SCHEMA!>participants<!>.lastName }

    <!SCHEMA!>fun test()<!> = <!SCHEMA!>dataFrameOf("a", "b")<!>(1, 2)
}
