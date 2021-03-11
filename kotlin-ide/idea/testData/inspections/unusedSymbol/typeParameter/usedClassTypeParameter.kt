class UsedClassTypeParameter<T>(t: T) {
    {
        println(t)
    }
}

fun main(args: Array<String>) {
    UsedClassTypeParameter("")
}