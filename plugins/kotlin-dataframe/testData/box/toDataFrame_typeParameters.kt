import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class ClassWithNullableAnyTypeArg<out T>(
    val v: T,
)

data class ClassWithAnyTypeArg<out T : Any>(
    val v: T,
)

fun f(l: List<ClassWithNullableAnyTypeArg<Int>>) {
    val df = l.toDataFrame()
    val v: Any? = df[0].v
    df.schema().print()
}

fun f1(l: List<ClassWithAnyTypeArg<Int>>) {
    val df = l.toDataFrame()
    val v: Any = df[0].v
    df.schema().print()
}

fun box(): String {
    f(listOf(ClassWithNullableAnyTypeArg(1)))
    f1(listOf(ClassWithAnyTypeArg(1)))
    return "OK"
}
