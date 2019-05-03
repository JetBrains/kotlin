internal class X : Cloneable {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }
}

internal class Y : Thread(), Cloneable {
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }
}