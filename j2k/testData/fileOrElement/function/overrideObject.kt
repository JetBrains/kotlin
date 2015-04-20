// ERROR: Unresolved reference: clone
class X {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }

    throws(CloneNotSupportedException::class)
    protected fun clone(): Any {
        return super.clone()
    }
}

class Y : Thread() {
    throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }
}