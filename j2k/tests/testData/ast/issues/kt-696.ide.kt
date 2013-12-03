package test

class Base() {
    public fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    public fun equals(o: Any): Boolean {
        return this.identityEquals(o)
    }

    public fun toString(): String {
        return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
    }
}
class Child() : Base() {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }
}