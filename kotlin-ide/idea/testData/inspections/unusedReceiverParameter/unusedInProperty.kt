val String.something: Int
    get() = 42

fun main(args: Array<String>) {
    "".something
}