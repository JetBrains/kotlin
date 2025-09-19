import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface A {
    val int: Int
}

@DataSchema
interface B {
    val group: A
}

fun box(): String {
    val df = dataFrameOf("int")(1)
        .group { int }.into("group")
        .cast<B>(verify = false)

    df[0].group.int

    return "OK"
}