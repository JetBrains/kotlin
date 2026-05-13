import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("a").nulls<Int>(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").fillIndexed(10) { i, name -> "$name-$i" }.let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").fill(10) { it * 2 }.let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").fill(10, "x").let {
        it.compareSchemas(strict = true)
    }
    return "OK"
}
