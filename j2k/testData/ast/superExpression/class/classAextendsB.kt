open class B(i : Int) {
open fun call() : Int {
return 1
}
}
open class A() : B(10) {
override fun call() : Int {
return super.call()
}
}