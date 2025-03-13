import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

public val df1 = dataFrameOf("a")(1).add("b") { 2 }
internal val df2 = dataFrameOf("a")(1).add("b") { 2 }
private val df3 = dataFrameOf("a")(1).add("b") { 2 }

class Container {
    public val df1 = dataFrameOf("a")(1).add("b") { 2 }
    internal val df2 = dataFrameOf("a")(1).add("b") { 2 }
    protected val df3 = dataFrameOf("a")(1).add("b") { 2 }
    private val df4 = dataFrameOf("a")(1).add("b") { 2 }
}

fun box(): String = "OK"
