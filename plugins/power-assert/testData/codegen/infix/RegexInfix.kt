// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Disabled because of KT-65640

fun box() = expectThrowableMessage {
    assert("Hello, World" matches "[A-Za-z]+".toRegex())
}
