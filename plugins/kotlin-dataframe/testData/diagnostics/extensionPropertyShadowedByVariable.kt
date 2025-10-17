import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface MySchema {
    val a: Int
}

fun box(): String {
    val df = dataFrameOf("a" to columnOf(42))
    val a = 123
    df.filter { <!DATAFRAME_EXTENSION_PROPERTY_SHADOWED!>a<!> == 42 }
    df.filter { it.a == 42 }
    df.filter { this.a == 42 }

    df.cast<MySchema>().filter { <!DATAFRAME_EXTENSION_PROPERTY_SHADOWED!>a<!> == 42 }
    df.cast<MySchema>().filter { this.a == 42 }
    df.cast<MySchema>().filter { it.a == 42 }
    return "OK"
}
