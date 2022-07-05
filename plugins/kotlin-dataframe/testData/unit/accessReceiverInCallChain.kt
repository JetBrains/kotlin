import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

fun test(testAbc: DataFrame<*>) {
    testAbc.add("age") { 42 }
}