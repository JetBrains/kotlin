// KJS_WITH_FULL_RUNTIME

import kotlin.js.worker.*
import kotlin.js.Promise

fun box(): Promise<String> {
    return worker {
        "OK"
    }.waitForReply()
}
