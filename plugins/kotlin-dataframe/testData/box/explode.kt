import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface ExplodeSchema {
    val timestamps: List<Int>
}

fun explode(df: DataFrame<ExplodeSchema>) {
    val res = df.explode { timestamps }
    val col: DataColumn<Int> = res.timestamps
}

fun box(): String {
    val df = dataFrameOf("timestamps")(listOf(100, 113, 140), listOf(400, 410, 453)).cast<ExplodeSchema>()
    val df1 = df.explode { timestamps }
    val timestamps: DataColumn<Int> = df1.timestamps
    timestamps.print()
    return "OK"
}
