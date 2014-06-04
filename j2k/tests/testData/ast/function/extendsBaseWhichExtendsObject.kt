package test

class Test() : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any): Boolean {
        return super.equals(o)
    }

    override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun finalize() {
        super.finalize()
    }
}

class Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(o)
    }

    protected fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    protected fun finalize() {
        super.finalize()
    }
}