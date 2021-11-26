import kotlinx.cinterop.staticCFunction
import cCallback.runAndCatch

fun throwingCallback() {
    throw IllegalStateException("Kotlin Exception!")
}


fun main() {
    runAndCatch(staticCFunction(::throwingCallback))
}