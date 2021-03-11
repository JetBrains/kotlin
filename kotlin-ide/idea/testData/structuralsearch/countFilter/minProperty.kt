class A {
    <warning descr="SSR">lateinit var x: String</warning>
    var y = 1

    fun init() { x = "a" }
}