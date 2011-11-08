open class B() {
open fun call() : Unit {
}
}
open class A() : B {
{
super()
}
override fun call() : Unit {
return super.call()
}
}