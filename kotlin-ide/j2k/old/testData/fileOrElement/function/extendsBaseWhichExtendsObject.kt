// ERROR: Unresolved reference: clone
// ERROR: Unresolved reference: finalize
package test

internal class Test : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    @Throws(Throwable::class)
    override fun finalize() {
        super.finalize()
    }
}

internal open class Base {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    @Throws(CloneNotSupportedException::class)
    protected open fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return super.toString()
    }

    @Throws(Throwable::class)
    protected open fun finalize() {
        super.finalize()
    }
}