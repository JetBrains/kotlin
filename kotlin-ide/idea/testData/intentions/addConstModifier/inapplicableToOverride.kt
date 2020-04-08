// IS_APPLICABLE: FALSE

interface I {
    val a: String
}
object O: I {
    override val <caret>a = ""
}