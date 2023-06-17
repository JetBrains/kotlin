// Checks that on Apple targets first two lines of exception stacktrace of symbolized executable looks like
// "\tat 1   main.kexe\t\t 0x000000010d7cdb4c kfun:package.function(kotlin.Int) + 108 (/path/to/file/name.kt:10:27)\n"
// If test is broken, org.jetbrains.kotlin.idea.filters.KotlinExceptionFilter (in main Kotlin repo) should be updated.

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

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

var depth = 3

fun main(args : Array<String>) {
    val sourceInfoType = args.first()
    val exceptionalFrames = when (sourceInfoType) {
        "libbacktrace" -> 0
        "coresymbolication" -> 2
        else -> throw AssertionError("Unknown source info type " + sourceInfoType)
    }
    depth += exceptionalFrames
    try {
        functionB()
    } catch (e: Throwable) {
        val stacktrace = e.getStackTrace()
	assert(stacktrace.size >= depth)
	stacktrace.take(depth).forEach(::checkStringFormat)
    }
}
