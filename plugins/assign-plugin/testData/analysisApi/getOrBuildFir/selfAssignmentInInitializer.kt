// IGNORE_FIR
// ISSUE: KT-78987
annotation class ValueContainer

@ValueContainer
class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
}

class Task {
    val member = member = "str"

    fun usage() {
        <expr>member</expr>.toString()
    }
}
