import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*


@DataSchema
data class Record(val a: String, val b: Int)

fun box(): String {
    val df = List(10) { Record(it.toString(), it) }.let { dataFrameOf(*it.toTypedArray()) }
    val aggregate = df.pivot { b }.aggregate {
        this.add("c") { 123 }.c
    }
    return "OK"
}
