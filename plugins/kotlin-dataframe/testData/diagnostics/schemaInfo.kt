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
    val <!PROPERTY_SCHEMA!>df<!> = listOf(
        Group("1", listOf(
            Person("Alice", "Cooper", 15, "London"),
            Person("Bob", "Dylan", 45, "Dubai")
        )),
        Group("2", listOf(
            Person("Charlie", "Daniels", 20, "Moscow"),
            Person("Charlie", "Chaplin", 40, "Milan"),
        )),
    ).<!FUNCTION_CALL_SCHEMA!>toDataFrame<!>(maxDepth = 2)

    // For operator get call schema is reported for the whole expression, so on df two schemas are reported
    <!FUNCTION_CALL_SCHEMA!><!PROPERTY_ACCESS_SCHEMA!>df<!>.participants[0]<!>

    <!PROPERTY_ACCESS_SCHEMA!>df<!>.participants.<!FUNCTION_CALL_SCHEMA!>first<!>()

    <!PROPERTY_ACCESS_SCHEMA!>df<!>.<!FUNCTION_CALL_SCHEMA!>explode<!> { participants }.<!PROPERTY_ACCESS_SCHEMA!>participants<!>

    <!FUNCTION_CALL_SCHEMA!><!PROPERTY_ACCESS_SCHEMA!>df<!>.participants[0]<!>.age

    // DataRow
    <!FUNCTION_CALL_SCHEMA!><!PROPERTY_ACCESS_SCHEMA!>df<!>[0]<!>.<!PROPERTY_ACCESS_SCHEMA!>participants<!>.age

    // GroupBy
    <!PROPERTY_ACCESS_SCHEMA!>df<!>.<!FUNCTION_CALL_SCHEMA!>explode<!> { participants }.<!FUNCTION_CALL_SCHEMA!>groupBy<!> { <!PROPERTY_ACCESS_SCHEMA!>participants<!>.lastName }

    <!FUNCTION_SCHEMA!>fun test()<!> = <!FUNCTION_CALL_SCHEMA!>dataFrameOf("a", "b")<!>(1, 2)
}
