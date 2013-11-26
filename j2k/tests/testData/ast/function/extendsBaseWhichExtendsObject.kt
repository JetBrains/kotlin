package test

open class Test() : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
    override fun clone(): Any? {
        return super.clone()
    }
    override fun toString(): String? {
        return super.toString()
    }
    override fun finalize() {
        super.finalize()
    }
}
open class Base() {
    public open fun hashCode(): Int {
        return System.identityHashCode(this)
    }
    public open fun equals(o: Any?): Boolean {
        return this.identityEquals(o)
    }
    protected open fun clone(): Any? {
        return super.clone()
    }
    public open fun toString(): String? {
        return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
    }
    protected open fun finalize() {
        super.finalize()
    }
}