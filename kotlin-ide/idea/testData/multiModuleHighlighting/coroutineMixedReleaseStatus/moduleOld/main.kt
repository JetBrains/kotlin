import libN.*
import libO.*

suspend fun oldMain() {
    <error descr="[VERSION_REQUIREMENT_DEPRECATION_ERROR] 'newFoo(): Unit' is only available since Kotlin 1.3 and cannot be used in Kotlin 1.2">newFoo</error>()
    oldFoo()
}

fun oldMain2() {
    <error descr="[VERSION_REQUIREMENT_DEPRECATION_ERROR] 'newBuilder(suspend () -> Unit): Unit' is only available since Kotlin 1.3 and cannot be used in Kotlin 1.2">newBuilder</error> {
        oldMain()
    }

    oldBuilder {
        oldMain()
    }
}
