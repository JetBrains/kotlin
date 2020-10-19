class MyClass()

abstract class MyAbstractClass {
    var a = MyClass()
    <warning descr="SSR">abstract var b: MyClass</warning>
    <warning descr="SSR">lateinit var c: MyAbstractClass</warning>
}