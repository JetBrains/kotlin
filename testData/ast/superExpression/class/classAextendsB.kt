open class B {
fun call() : Unit {
}
}
open class A : B {
override fun call() : Unit {
return super<B>.call()
}
}