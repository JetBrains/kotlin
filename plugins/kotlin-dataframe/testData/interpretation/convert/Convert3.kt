import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert3 {
    val age: Number?
    val city: String?
    val name: String?
    val weight: Int?
}

fun convert3(df: DataFrame<Convert3>) {
    val df1 = df.convert("age") { "age"<Number?>()?.toDouble() }.cast<Int>()
    fun col0(v: Double?) {}
    col0(df1.age[0])
}