import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "group" to columnOf(
            "nested" to columnOf(123321),
            "nested1" to columnOf("123321"),
            "nested2" to columnOf("123321"),
        ),
        "group1" to columnOf(
            "nested" to columnOf(456),
            "nested1" to columnOf(456),
        )
//        "frameCol" to columnOf(
//            dataFrameOf("m" to columnOf(1, 2, 3))
//        )
    )

//    df.gather { group }.into("key", "value")
//    df.gather { group }.explodeLists().into("key", "value")
    df.gather { group and group1 }.into("key", "value")
    return "OK"
}
