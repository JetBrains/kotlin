import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Schema {
    val a: Int
}

fun box(): String {
    val res = dataFrameOf("a")(1)
        .cast<Schema>()
        .add("wwffffwwehirbwerffwffwffwfffffwfffwfwfwfaw") { 42 }

    res.wwffffwwehirbwerffwffwffwfffffwfffwfwfwfaw.print()
    res.a.print()

    val b = res.convert { a }.with { it.toString() }
    
    b.a

//    val res1 = res.conv
    //res.filter { it }
    return "OK"
}
