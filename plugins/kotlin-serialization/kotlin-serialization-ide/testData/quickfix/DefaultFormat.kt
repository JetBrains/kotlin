// "Replace by default Json format" "true"
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun foo() {
    <caret>Json {}.encodeToString(Any())
}
