import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1).add {
        "id" from { it }
        "group" {
            "a" from { it }
        }
        group("group1") {
            "b" from { it }
        }
    }

    df.group.a
    df.group1.b
    return "OK"
}
