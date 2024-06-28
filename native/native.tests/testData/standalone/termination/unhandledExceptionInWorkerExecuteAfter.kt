// EXIT_CODE: !0
// OUTPUT_REGEX: .*an error.*
// OUTPUT_REGEX: (?!.*Will not happen.*).*
import kotlin.test.*

import kotlin.native.concurrent.*

fun main() {
    val worker = Worker.start()
    worker.executeAfter(0L, {
        throw Error("an error")
    })
    worker.requestTermination().result
    println("Will not happen")
}