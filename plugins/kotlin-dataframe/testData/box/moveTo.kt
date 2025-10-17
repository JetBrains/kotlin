import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    val res = df.move { c and a }.to(3).select { drop(1) }
    res.compareSchemas(strict = true)
    val res1 = df.moveTo(3) { c and a }.select { drop(1) }
    res1.compareSchemas(strict = true)
    return "OK"
}
