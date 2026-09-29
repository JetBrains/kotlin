// ISSUE: KT-78987
annotation class ValueContainer

@ValueContainer
class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
}

val topLevel = topLevel = "str"

class Task {
    val member = member = "str"

    fun usage() {
        member.toString()
    }
}
