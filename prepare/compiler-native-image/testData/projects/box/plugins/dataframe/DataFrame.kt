// COMPILER_PLUGIN: org.jetbrains.kotlin.dataframe dataframe-compiler-plugin.*.jar
// FULL_JDK
// WITH_REFLECT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df = dataFrameOf("a")(1).addId()
    val id: DataColumn<Int> = df.id
    return if (id.size == 1) "OK" else "fail: unexpected id column size ${id.size}"
}
