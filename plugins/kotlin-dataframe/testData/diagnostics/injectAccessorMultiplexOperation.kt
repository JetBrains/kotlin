import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

@DataSchema
interface Schema {
    val a: Int
    val b: Int
    val c: Int
}

fun `convert intermediate object is evaluated`(df: DataFrame<Schema>) {
    df.convert("a", "b", "c").with { it.toString() }.forEach {
        consumeStr(a)
    }
}

fun consumeStr(s: String) {

}