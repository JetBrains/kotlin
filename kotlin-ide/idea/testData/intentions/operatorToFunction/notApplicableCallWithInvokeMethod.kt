// IS_APPLICABLE: false
class coffee() {
    fun invoke() {
    }
}
fun main() {
    val f = coffee().invoke<caret>()
}