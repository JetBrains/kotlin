// FILE: Test.kt
package org.jetbrains.kotlinx.dataframe

import kotlin.random.Random
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.explode
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.with

@DataSchema
interface Schema {
    val a: Int
}

@DataSchema
interface Log {
    val timestamp: Long
    val message: String
}

//fun testCast(): DataFrame<Log> {
//    val df = DataFrame.empty(10)
//        .add("timestamp") { Random.nextLong() }
//        .add("message") { "$timestamp: diagnostic ..." }
//
//    return df.cast<Log>()
//}

fun main(args: Array<String>) {
    val res = dataFrameOf("a")(1)
        .cast<Schema>()
        .add("ba") { 42 }

    res.ba.print()
    res.a.print()

    val a = res.convert { a }.with { it.digitToChar() }

    val str: DataColumn<Char> = a.a



    print(str)

//    println(org.jetbrains.kotlinx.dataframe.testCast())

//    val res1 = res.conv
    //res.filter { it }
}

// FILE: duplicatedSignature.kt

package org.jetbrains.kotlinx.dataframe

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read

//@DataSchema
//interface Log {
//    val timestamp: Long
//    val message: String
//}

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
    // df.timestamp
    val df1 = df
        .convert { timestamp }.with { LocalDateTime.parse(it, format)!! }
//        .add("tsDiff") { diff(ChronoUnit.MINUTES) { timestamp }?.let { it > 20  } ?: true }
//        .add("charDiff") { diff { char }?.let { it != 0 } ?: true }

    df1.print()
    return "OK"
}
