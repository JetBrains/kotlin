import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema(isOpen = false)
interface Convert61 {
    val age: Number?
    val name: String?
    val weight: Int?
}

@DataSchema
interface Convert6 {
    val city: String?
    val person: DataRow<Convert61>
}

fun convert6(df: DataFrame<Convert6>) {
    val df1 = df.convert { person.age }.with { it ?: 42 }.cast<Int>()
    fun col0(v: Number) {}
    col0(df1.person.age[0])
}