// DUMP_SCHEMAS
//
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun test() {
    <!FUNCTION_SCHEMA!>fun AnyFrame.aggregate()<!> = run {
        val <!PROPERTY_SCHEMA!>df<!> = <!FUNCTION_CALL_SCHEMA!>dataFrameOf<!>("a" to columnOf(42))
        <!PROPERTY_ACCESS_SCHEMA!>df<!> <!FUNCTION_CALL_SCHEMA!>to<!> <!PROPERTY_ACCESS_SCHEMA!>df<!>
    }

    val <!PROPERTY_SCHEMA!>df1<!> = <!FUNCTION_CALL_SCHEMA!>dataFrameOf<!>("test" to columnOf(123))

    val (<!PROPERTY_SCHEMA!>_<!>, <!PROPERTY_SCHEMA!>b<!>) = <!PROPERTY_ACCESS_SCHEMA!>df1<!>.<!FUNCTION_CALL_SCHEMA!>aggregate<!>()
}
