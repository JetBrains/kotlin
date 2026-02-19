import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Repo {
    val name: String
    val url: String
}
//
@DataSchema
data class Type(val name: String, val vararg: Boolean)

fun box() = "OK"
