import namespace.deeper.*

fun recieve_class(arg: Class_without_package): Unit = TODO()
fun produce_class(): Class_without_package = TODO()

fun recieve_class_wp(arg: Class_with_package): Unit = TODO()
fun produce_class_wp(): Class_with_package = TODO()

fun recieve_object(arg: Object_without_package): Unit = TODO()
fun produce_object(): Object_without_package = TODO()

fun recieve_object_wp(arg: Object_with_package): Unit = TODO()
fun produce_object_wp(): Object_with_package = TODO()

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
}
