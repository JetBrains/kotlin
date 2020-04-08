// WITH_RUNTIME
interface T

abstract class <caret>B: T {
    // INFO: {"checked": "true"}
    inner class X {

    }
}