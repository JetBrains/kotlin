package test

open class Base() {
    public open fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    public open fun equals(o: Any?): Boolean {
        return this.identityEquals(o)
    }

    public open fun toString(): String? {
        return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
    }
}

open class Child() : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String? {
        return super.toString()
    }
}