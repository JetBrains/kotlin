import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import kotlin.reflect.KProperty

fun test(kProperty0: KProperty<Int>) {}

interface Schema {
    val i: Int

    @ColumnName("hello")
    val qfewef: Int
}

val i = 42

fun call() {
    test(::i)
    test(Schema::i)
    test(Schema::qfewef)
}