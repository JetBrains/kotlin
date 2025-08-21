import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1).mapToFrame {
        "id" from { it }
        "group" {
            "a" from { it }
        }
    }

    df.id
    df.group.a

    df.compareSchemas(strict = true)
    return "OK"
}
