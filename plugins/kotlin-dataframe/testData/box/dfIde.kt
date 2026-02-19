import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Schema {
    val i: Int
    val fff: String
}

fun main(args: Array<String>) {
    val df = dataFrameOf("i", "fff")(1, "321").cast<Schema>()
    println(df.i)

    val df1 = df.add("ca") { 423 }
    val res = df1.ca
    df1.filter { it.ca == 12 }

    `Name is evaluated to age`(dataFrameOf("a")(123).cast())
}

interface Cars

fun `Name is evaluated to age`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 }
    val col = df1.age
    println(col)
}

fun box() = "OK"
