package lib

var initLog = ""

fun mark(text: String): String {
    initLog += text
    return text
}
