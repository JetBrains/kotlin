// "Replace with default Json format instance" "true"
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun foo() {
    <caret>Json {}.encodeToString(Any())
}
