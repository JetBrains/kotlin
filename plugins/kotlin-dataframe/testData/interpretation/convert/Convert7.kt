import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert7 {
    val age: Number?
    val city: String?
    val name: String?
    val weight: Int?
}

fun convert7(df: DataFrame<Convert7>) {
    val df1 = df.convert { age }.to<String?>().cast<Int>()
    fun col0(v: String?) {}
    col0(df.age[0])
}