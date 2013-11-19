package test
class Test() : Base() {
public override fun hashCode() : Int {
return super.hashCode()
}
public override fun equals(o : Any) : Boolean {
return super.equals(o)
}
protected override fun clone() : Any {
return super.clone()
}
public override fun toString() : String {
return super.toString()
}
protected override fun finalize() {
super.finalize()
}
}
class Base() {
public fun hashCode() : Int {
return System.identityHashCode(this)
}
public fun equals(o : Any) : Boolean {
return this.identityEquals(o)
}
protected fun clone() : Any {
return super.clone()
}
public fun toString() : String {
return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
}
protected fun finalize() {
super.finalize()
}
}