package test;

abstract class A {
    abstract val a: Int
    abstract var b: Int
        protected set
    val c: Int = TODO()
    val d: Int
        get() = TODO()
    var e: Int
        get() = TODO()
        set(value) = TODO()
    var f: Int = TODO()
        private set
    open val g: Int = TODO()
    open val h: Int
        get() = TODO()
    open var k: Int
        get() = TODO()
        set(value) = TODO()
    open var l: Int = TODO()
        protected set
}