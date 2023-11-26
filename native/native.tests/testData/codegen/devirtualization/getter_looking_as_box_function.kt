// OUTPUT_DATA_FILE: getter_looking_as_box_function.out
class Foo(val box: String = "box")

fun box(): String {
    println(Foo().box)

    return "OK"
}
