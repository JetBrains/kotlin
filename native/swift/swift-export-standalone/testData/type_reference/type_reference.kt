// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
import namespace.deeper.*

fun recieve_class(arg: Class_without_package): Unit = TODO()
fun produce_class(): Class_without_package = TODO()
val val_class: Class_without_package
    get() = TODO()
var var_class: Class_without_package
    get() = TODO()
    set(value) = TODO()

fun recieve_class_wp(arg: Class_with_package): Unit = TODO()
fun produce_class_wp(): Class_with_package = TODO()
val val_class_wp: Class_with_package
    get() = TODO()
var var_class_wp: Class_with_package
    get() = TODO()
    set(value) = TODO()

fun recieve_object(arg: Object_without_package): Unit = TODO()
fun produce_object(): Object_without_package = TODO()
val val_object: Object_without_package
    get() = TODO()
var var_object: Object_without_package
    get() = TODO()
    set(value) = TODO()

fun recieve_object_wp(arg: Object_with_package): Unit = TODO()
fun produce_object_wp(): Object_with_package = TODO()
val val_object_wp: Object_with_package
    get() = TODO()
var var_object_wp: Object_with_package
    get() = TODO()
    set(value) = TODO()

fun combine(
    arg1: Class_without_package,
    arg2: Class_with_package,
    arg3: Object_without_package,
    arg4: Object_with_package,
): Unit = TODO()

class Demo(
    val arg1: Class_without_package,
    val arg2: Class_with_package,
    val arg3: Object_without_package,
    val arg4: Object_with_package,
) {
    class INNER_CLASS
    object INNER_OBJECT
    fun combine(
        arg1: Class_without_package,
        arg2: Class_with_package,
        arg3: Object_without_package,
        arg4: Object_with_package,
    ): Demo = TODO()
    fun combine_inner_classses(
        arg1: Class_without_package.INNER_CLASS,
        arg2: Class_with_package.INNER_CLASS,
        arg3: Object_without_package.INNER_CLASS,
        arg4: Object_with_package.INNER_CLASS,
    ): INNER_CLASS = TODO()
    fun combine_inner_objects(
        arg1: Class_without_package.INNER_OBJECT,
        arg2: Class_with_package.INNER_OBJECT,
        arg3: Object_without_package.INNER_OBJECT,
        arg4: Object_with_package.INNER_OBJECT,
    ): INNER_OBJECT = TODO()

    var var1: Class_without_package
        get() = TODO()
        set(value) = TODO()
    var var2: Class_with_package
        get() = TODO()
        set(value) = TODO()
    var var3: Object_without_package
        get() = TODO()
        set(value) = TODO()
    var var4: Object_with_package
        get() = TODO()
        set(value) = TODO()
}


// FILE: ingored.kt
package ignored

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

// FILE: predefined_type_with_package.kt
package namespace.deeper

class Class_with_package {
    class INNER_CLASS
    object INNER_OBJECT
}
object Object_with_package {
    class INNER_CLASS
    object INNER_OBJECT
}

// FILE: predefined_type_without_package.kt
class Class_without_package {
    class INNER_CLASS
    object INNER_OBJECT
}
object Object_without_package {
    class INNER_CLASS
    object INNER_OBJECT
}

