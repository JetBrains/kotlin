import kotlinx.cinterop.*
import kotlin.test.*
import mangling.*

fun main() {
    companion = _Companion.`Companion$`
    assertEquals(_Companion.`Companion$`, companion)
}

