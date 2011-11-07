open class B {
open fun call() : Unit {
}
}
open class A : B {
override fun call() : Unit {
return super.call()
}
}