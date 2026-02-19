package foo

fun box(): String {
    return "Fail${return "OK"}${return "Fail2"}"
}
