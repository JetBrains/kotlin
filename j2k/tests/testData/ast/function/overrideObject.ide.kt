package test
class Test() {
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
return getJavaClass<Test>.getName() + '@' + Integer.toHexString(hashCode())
}
protected fun finalize() {
super.finalize()
}
}