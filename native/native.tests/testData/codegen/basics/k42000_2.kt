import kotlin.test.*

// https://youtrack.jetbrains.com/issue/KT-42000

fun box(): String {
    assertFailsWith<Error> {
        when (1) {
            else -> throw Error()
        } as String
    }

    return "OK"
}
