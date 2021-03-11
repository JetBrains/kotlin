// IS_APPLICABLE: false
class A {
    var b<caret>: Int

    init {
        val i = 0
        b = i
    }
}
