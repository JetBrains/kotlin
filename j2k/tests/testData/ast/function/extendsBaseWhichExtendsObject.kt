package test

class Test : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    throws(javaClass<CloneNotSupportedException>())
    override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    throws(javaClass<Throwable>())
    override fun finalize() {
        super.finalize()
    }
}

class Base {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    throws(javaClass<CloneNotSupportedException>())
    protected fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    throws(javaClass<Throwable>())
    protected fun finalize() {
        super.finalize()
    }
}