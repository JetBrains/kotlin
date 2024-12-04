@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package pkg.lib1

interface InterfaceClash
class ClassClash

interface InterfaceClash2
class ClassClash2

@ObjCName("InterfaceClash2")
interface InterfaceClashWithObjCName
@ObjCName("ClassClash2")
class ClassClashWithObjCName

class Lib1Kt

enum class E {
    ONE,
    one,
    @ObjCName("one")
    TWO,
    ENTRIES,
    @ObjCName("values") VALUES1;
}

interface I1 {
    val prop: Int
    fun method()
}

interface I2 {
    val prop: String
    fun method()
}

fun topLevel(arg: Int) {}
fun topLevel(arg: Long) {}