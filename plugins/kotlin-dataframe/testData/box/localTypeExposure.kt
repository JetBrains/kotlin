import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

private fun df3() = dataFrameOf("a")(1).add("b") { 2 }

class Container {
    private fun df4() = dataFrameOf("a")(1).add("b") { 2 }
}

fun box(): String = "OK"
