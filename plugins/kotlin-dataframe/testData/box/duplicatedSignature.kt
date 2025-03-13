// FILE: Test.kt
import kotlin.random.Random
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Schema {
    val a: Int
}

@DataSchema
interface Log {
    val timestamp: Long
    val message: String
}

fun main(args: Array<String>) {
    val res = dataFrameOf("a")(1)
        .cast<Schema>()
        .add("ba") { 42 }

    res.ba.print()
    res.a.print()

    val a = res.convert { a }.with { it.digitToChar() }

    val str: DataColumn<Char> = a.a
    print(str)
}

// FILE: duplicatedSignature.kt

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface ActivePlayer {
    val char: Int
    val level: Int
    val race: String
    val charclass: String
    val zone: String
    val guild: Int
    val timestamp: String
}


fun box(): String {
    val df = dataFrameOf("char", "level", "race", "charclass", "zone", "guild", "timestamp")(59425,1,"Orc","Rogue","Orgrimmar",165,"01/01/08 00:02:04").cast<ActivePlayer>(verify = true)
    val format = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")
    val df1 = df
        .convert { timestamp }.with { LocalDateTime.parse(it, format)!! }

    df1.print()
    return "OK"
}
