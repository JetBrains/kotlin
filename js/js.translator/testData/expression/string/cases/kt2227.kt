package foo

fun box(): Boolean {
    if ("${3}" != "3") return false
    return "${3}" == "3"
}