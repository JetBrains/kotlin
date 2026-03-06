import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

// TODO

fun AnyFrame.asNothing() = this as DataFrame<Nothing>

fun box(): String {
    val df = dataFrameOf("col")("1", "2").convert { col }.to<Int>()
    val a = df.asNothing()
    val i: Int = df.col[0]
    return "OK"
}

fun box1(): String {
    val df = dataFrameOf("col")("1", "2").convert { col }.to<Int>()
    val b = TODO("test")
    val i: Int = df.col[0]
    return "OK"
}

fun box2(): String {
    val df = dataFrameOf("col")("1", "2").convert { col }.to<Int>()
    val a = df.asNothing()
    val b = TODO("test")
    val i: Int = df.col[0]
    return "OK"
}
