class A {
    <warning descr="SSR">lateinit var x: String</warning>
    <warning descr="SSR">var y = 1</warning>

    fun init() { x = "a" }
}