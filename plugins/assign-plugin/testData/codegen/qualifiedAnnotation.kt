// FILE: annotation.kt
package qualified

annotation class ValueContainer

// FILE: main.kt
import qualified.ValueContainer

@ValueContainer
class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
}

val property = StringProperty("Initial")

fun box(): String {
    property = "OK"

    return property.v
}
