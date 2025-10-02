import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class Holder<T>(val value: T)

private fun getMyDf() = Holder(dataFrameOf("a", "b")(1, "test"))

fun box(): String {
    val i: Int = getMyDf().value.a[0]
    return "OK"
}

private fun getDeeplyNestedDf() = Holder(Holder(Holder(dataFrameOf("a", "b")(1, "test"))))

fun test() {
    val i: Int = getDeeplyNestedDf().value.value.value.a[0]
}