import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = dataFrameOf("a", "b", "frameCol")(1, 2, dataFrameOf("e", "f")(3, 4))

    compareSchemas(
        df1.select { frameCol },
        df1.select { frameCols() },
        df1.select { nameContains("a").frameCols() },
    )

    val into = df1.group { a and frameCol }.into("c")
    compareSchemas(
        into.select { c.frameCol },
        into.select { c.frameCols() },
    )
    return "OK"
}
