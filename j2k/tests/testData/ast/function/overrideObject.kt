class X() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }

    protected fun clone(): Any {
        return super.clone()
    }
}

class Y() : Thread() {
    override fun clone(): Any {
        return super.clone()
    }
}