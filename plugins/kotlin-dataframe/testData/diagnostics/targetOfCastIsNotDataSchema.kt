import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

interface MySchema {
    val a: String
}
fun box(): String {
    val df = DataFrame.Empty
    df.<!CAST_TARGET_WARNING!>cast<!><MySchema>()
    return "OK"
}
