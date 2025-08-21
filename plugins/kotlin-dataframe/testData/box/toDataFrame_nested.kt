import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = listOf("a").toDataFrame {
        "group" {
            "anotherGroup" {
                "a" from { 1 }
            }
            "b" from { "c" }
        }
    }

    df.group.b
    df.group.anotherGroup.a
    return "OK"
}
