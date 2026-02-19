import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("timestamps")(listOf(100, 113, 140), listOf(400, 410, 453))
    val df1 = df.explode { timestamps }
    val timestamps: DataColumn<Int> = df1.timestamps
    timestamps.print()


    val df2 = dataFrameOf("a", "b")(listOf(100, 113, 140), listOf(400, 410))
    val df3 = df2.explode { a and b }
    // exploding multiple columns will introduce nulls
    df3.print()
    // compiler needs to play safe and make both selected columns nullable
    df3.compileTimeSchema().columns.let {
        assert(it["a"]!!.nullable)
        assert(it["b"]!!.nullable)
    }
    // compile time schema is still compatible with runtime
    df3.compareSchemas()
    return "OK"
}
