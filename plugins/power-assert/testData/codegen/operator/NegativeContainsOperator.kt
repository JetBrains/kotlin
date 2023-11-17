// IGNORE_BACKEND_K2: JVM_IR
// K2 disabled because of KT-65636

fun box() = expectThrowableMessage {
    assert("Hello" !in listOf("Hello", "World"))
}
