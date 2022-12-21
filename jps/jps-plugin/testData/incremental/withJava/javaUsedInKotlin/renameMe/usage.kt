fun main() {
    val call = JavaClass::foo
    println(call.invoke(JavaClass()))
}