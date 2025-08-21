import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    val dCol: DataColumn<String> = df.insert("d") { (b * c).toString() }.at(2)
        // weird way to check order, but ok?..
        .select { drop(2) }
        .select { take(1) }
        .select { d }.d

    return "OK"
}
