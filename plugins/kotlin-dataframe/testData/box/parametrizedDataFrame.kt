import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Schema {
    val i: Int
}

fun box(): String {
    val df = dataFrameOf("i")(123, 321).cast<Schema>().add("new") { "a" }
    df.new.print()
    df.i.print()
    return "OK"
}
