class NoParams() {
    fun equals() = Unit
}

class WithParams() {
    fun equals(param: Int) = Unit
    fun hashCode(param: Int): Int = 42
    fun toString(param: Int): String = ""
}

class Override() {
    override fun equals(other: Any?): Boolean = super.equals(other)
    override fun hashCode(): Int = super.hashCode()
    override fun toString(): String = super.toString()
}

class Mix() {
    fun equals() = Unit
    fun equals(param: Int) = Unit
    override fun equals(other: Any?): Boolean = super.equals(other)

    fun hashCode(param: Int): Int = 42
    override fun hashCode(): Int = super.hashCode()

    fun toString(param: Int): String = ""
    override fun toString(): String = super.toString()
}