import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

interface SupertypeProperty {
    val i: Int
}

interface MyInterface : SupertypeProperty {
    val a: Int
}

class MyImpl(override val i: Int, override val a: Int) : MyInterface

fun test(list: List<MyInterface>) {
    val df = list.toDataFrame()
    df.schema().print()
    df.i
    df.a
}

fun box(): String {
    test(listOf(MyImpl(1, 2)))
    return "OK"
}
