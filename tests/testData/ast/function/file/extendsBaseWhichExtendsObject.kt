package test
open class Test() : Base() {
public override fun hashCode() : Int {
return super.hashCode()
}
public override fun equals(o : Any?) : Boolean {
return super.equals(o)
}
protected override fun clone() : Any? {
return super.clone()
}
public override fun toString() : String? {
return super.toString()
}
protected override fun finalize() : Unit {
super.finalize()
}
}
open class Base() {
public open fun hashCode() : Int {
return System.identityHashCode(this)
}
public open fun equals(o : Any?) : Boolean {
return this.identityEquals(o)
}
protected open fun clone() : Any? {
return super.clone()
}
public open fun toString() : String? {
return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
}
protected open fun finalize() : Unit {
super.finalize()
}
}