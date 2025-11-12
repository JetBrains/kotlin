package hair.test

data class Var(val id: Any) {
    override fun toString() = "Var($id)"

    companion object {
        private var nextId = 0
        fun nextNumbered() = Var(nextId.also { nextId = it.inc() })
    }
}