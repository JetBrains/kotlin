class Test {
    override fun equals(other: Any?): Boolean {
        if (this?.equals<caret>(other)) return true
        return false
    }
}