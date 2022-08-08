import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert0 {
    val year: Int
}

fun convert0(df: DataFrame<Convert0>) {
    val df1 = df.convert("year").to<String>().cast<Any>()
    fun col0(v: kotlin.String) {}
    col0(df1.year[0])
}
