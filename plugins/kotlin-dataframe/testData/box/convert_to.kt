import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("col")("1", "2").convert { col }.to<Int>().let { df ->
        val i: Int = df.col[0]
    }
    dataFrameOf("col")("1", "2").convert { col }.to<Int>(ParserOptions()).let { df ->
        val i: Int = df.col[0]
    }
    return "OK"
}
