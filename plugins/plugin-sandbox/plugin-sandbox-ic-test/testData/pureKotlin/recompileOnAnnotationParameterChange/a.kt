package foo

var result = "Error"

fun functionToCall(): String {
    result = "OK"
    return "OK"
}

fun anotherFunction(): String {
    result = "Another"
    return "Another"
}
