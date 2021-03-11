open class A
class B : A()
class C : A()

<warning descr="SSR">val b: B = B()</warning>
val c: C? = null