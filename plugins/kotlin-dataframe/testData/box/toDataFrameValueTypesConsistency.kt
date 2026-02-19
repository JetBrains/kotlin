import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

interface MyEmptyDeclaration

class MyEmptyDeclarationImpl : MyEmptyDeclaration

class Test(
    val name: String,
    val containingDeclaration: List<MyEmptyDeclaration>
)

fun box(): String {
    listOf(MyEmptyDeclarationImpl()).toDataFrame(maxDepth = 2).let { df ->
        val v: MyEmptyDeclaration = df.value[0]
        df.compareSchemas(strict = true)
    }

    listOf(MyEmptyDeclarationImpl()).toDataFrame {
        properties(maxDepth = 2)
    }.let { df ->
        val v: MyEmptyDeclaration = df.value[0]
        df.compareSchemas(strict = true)
    }

    listOf(Test("Test1", listOf(MyEmptyDeclarationImpl()))).toDataFrame(maxDepth = 2).let { df ->
        val v: MyEmptyDeclaration = df.containingDeclaration[0].value[0]
        df.compareSchemas(strict = true)
    }
    return "OK"
}
