package test

const val CONST = "foo"

class Klass {
    companion object {
        private val CHANGED = "old"
        const public val UNCHANGED = 100
    }
}

object Obj : Any() {
    private val CHANGED = "old:Obj"
    public val UNCHANGED = 200
}
