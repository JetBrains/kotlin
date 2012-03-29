package test
open class Test() : Base() {
public override fun hashCode() : Int {
return super.hashCode()
}
public override fun equals(o : Any?) : Boolean {
return super.equals(o)
}
override protected fun clone() : Any? {
return super.clone()
}
public override fun toString() : String? {
return super.toString()
}
override protected fun finalize() : Unit {
super.finalize()
}
}
open class Base() {
open public fun hashCode() : Int {
return System.identityHashCode(this)
}
open public fun equals(o : Any?) : Boolean {
return this.identityEquals(o)
}
open protected fun clone() : Any? {
return super.clone()
}
open public fun toString() : String? {
return getJavaClass<Base>.getName() + '@' + Integer.toHexString(hashCode())
}
open protected fun finalize() : Unit {
super.finalize()
}
}