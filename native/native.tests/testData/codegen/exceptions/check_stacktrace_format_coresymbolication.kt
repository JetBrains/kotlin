// FREE_COMPILER_ARGS: -Xbinary=sourceInfoType=coresymbolication
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT

import kotlin.test.*
import kotlin.text.Regex

val EXTENSION = ".kt:"
val LOCATION_PATTERN = Regex("\\d+:\\d+")

fun checkStringFormat(s: String) {
    val trimmed = s.trim()

    assertTrue(trimmed.endsWith(')'), "Line is not ended with ')'")
    assertNotEquals(trimmed.indexOf('('), -1, "No '(' before filename")

    val fileName = trimmed.substring(trimmed.lastIndexOf('(') + 1, trimmed.lastIndex)
    assertNotEquals(fileName.indexOf(EXTENSION), -1, "Filename 'kt' extension is absent")

    val location = fileName.substring(fileName.indexOf(EXTENSION) + EXTENSION.length)
    assertTrue(LOCATION_PATTERN.matches(location), "Expected location of form 12:8")
}

fun functionA() {
    throw Error("an error")
}

fun functionB() {
    functionA()
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun box(): String {
    val e = assertFailsWith<Throwable>(block = ::functionB)
    val stacktrace = e.getStackTrace()
    val depth = 5
    assertTrue(stacktrace.size >= depth)
    stacktrace.take(depth).forEach(::checkStringFormat)
    return "OK"
}
