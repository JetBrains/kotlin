// WITH_STDLIB
// API_VERSION: LATEST
// OPT_IN: kotlin.uuid.ExperimentalUuidApi

package a

import kotlin.uuid.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Holder(val u: Uuid)

fun box(): String {
    val h = Holder(Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1"))
    val msg = Json.encodeToString(h)
    return if (msg == """{"u":"bc501c76-d806-4578-b45e-97a264e280f1"}""") "OK" else "FAIL: $msg"
}

