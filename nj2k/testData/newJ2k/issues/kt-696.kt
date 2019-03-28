package test

internal open class Base {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }
}

internal class Child : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }
}