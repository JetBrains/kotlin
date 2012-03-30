package test
open class Test() {
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
return getJavaClass<Test>.getName() + '@' + Integer.toHexString(hashCode())
}
protected open fun finalize() : Unit {
super.finalize()
}
}