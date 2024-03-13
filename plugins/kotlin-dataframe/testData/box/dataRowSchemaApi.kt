import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.*

@DataSchema
class Schema(
    val a: Int,
    val b: String
)

fun box(): String {
    val df = dataFrameOf(Schema(1, "foo")).append(Schema(2, "bar"))
    df.print()
    return "OK"
}
