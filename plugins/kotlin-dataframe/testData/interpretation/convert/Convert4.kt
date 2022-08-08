import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert4 {
    val age: Number?
    val city: String?
    val name: String?
    val weight: Int?
}

fun convert4(df: DataFrame<Convert4>) {
    val df1 = df.convert("age", "weight") { it?.toString() }
    fun col0(v: String?) {}
    col0(df1.age[0])
    col0(df1.weight[0])
}
