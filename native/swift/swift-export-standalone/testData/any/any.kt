// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

object MyObject

fun getMainObject(): Any = MyObject
fun isMainObject(obj: Any): Boolean = obj == MyObject

// FILE: opaque.kt
package opaque

data class DATA_CLASS(val a: Int)
value class VALUE_CLASS(val a: Int)
open class OPEN_CLASS
abstract class ABSTRACT_CLASS
interface INTERFACE

data object DATA_OBJECT {
    val a: Int = 42
}

enum class ENUM {
    A,
}

fun recieve_DATA_CLASS(x: DATA_CLASS): Unit = TODO()
fun produce_DATA_CLASS(): DATA_CLASS = TODO()

fun recieve_VALUE_CLASS(x: VALUE_CLASS): Unit = TODO()
fun produce_VALUE_CLASS(): VALUE_CLASS = TODO()

fun recieve_OPEN_CLASS(x: OPEN_CLASS): Unit = TODO()
fun produce_OPEN_CLASS(): OPEN_CLASS = TODO()

fun recieve_ABSTRACT_CLASS(x: ABSTRACT_CLASS): Unit = TODO()
fun produce_ABSTRACT_CLASS(): ABSTRACT_CLASS = TODO()

fun recieve_INTERFACE(x: INTERFACE): Unit = TODO()
fun produce_INTERFACE(): INTERFACE = TODO()

fun recieve_DATA_OBJECT(x: DATA_OBJECT): Unit = TODO()
fun produce_DATA_OBJECT(): DATA_OBJECT = TODO()

fun recieve_ENUM(x: ENUM): Unit = TODO()
fun produce_ENUM(): ENUM = TODO()
