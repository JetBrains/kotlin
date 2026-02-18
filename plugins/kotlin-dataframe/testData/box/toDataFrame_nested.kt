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

    val s: String = df[0].group.b
    val i: Int = df[0].group.anotherGroup.a
    df.compareSchemas(strict = true)
    return "OK"
}
