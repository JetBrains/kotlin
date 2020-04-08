// properties can be referred by component1/component2, which is too expensive to search, don't mark them as unused
data class MyClass(val param1: String, var param2: String)

fun main(args: Array<String>) {
    MyClass()
}
