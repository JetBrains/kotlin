import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.random.Random

fun box(): String {
    val l: List<Int>? = if (Random.nextBoolean()) listOf(123) else null

    l<!UNSAFE_CALL!>.<!>toDataFrame {

    }
    return "OK"
}
