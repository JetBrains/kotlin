package test
open class Test() {
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
return getJavaClass<Test>.getName() + '@' + Integer.toHexString(hashCode())
}
open protected fun finalize() : Unit {
super.finalize()
}
}