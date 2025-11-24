import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class MyEmptyDeclaration

class TestItem(val name: String, val containingDeclaration: MyEmptyDeclaration, val test: Int)

fun box(): String {
    List(10) {
        TestItem(
            "Test1",
            MyEmptyDeclaration(),
            123,
        )
    }.toDataFrame(maxDepth = 2).let { df ->
        val v: MyEmptyDeclaration = df.containingDeclaration[0]
        df.compareSchemas()
    }

    listOf(MyEmptyDeclaration(), MyEmptyDeclaration()).toDataFrame().let { df ->
        val v: MyEmptyDeclaration = df.value[0]
        df.compareSchemas()
    }

    return "OK"
}
