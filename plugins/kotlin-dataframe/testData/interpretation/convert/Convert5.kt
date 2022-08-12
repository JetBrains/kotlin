import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert5 {
    val age: Number?
    val city: String?
    val name: String?
    val weight: Int?
}

fun convert5(df: DataFrame<Convert5>) {
    val df1 = df.convert { age }.with { 42 }.cast<Int>()
    fun col0(v: Int) {}
    col0(df.age[0])
}
