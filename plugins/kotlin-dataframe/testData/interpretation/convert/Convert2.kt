import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert2 {
    val a: Int
    val b: Int
    val c: Int
}

fun convert2(df: DataFrame<Convert2>) {
    val df1 = df.convert("a", "b", "c").with { it.toString() }.cast<Int>()

    fun col0(v: String) {}
    col0(df1.a[0])
    col0(df1.b[0])
    col0(df1.c[0])
}