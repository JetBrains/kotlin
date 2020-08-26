class A {
    fun ssssa(): Int {
        b()
        return 1
    }
    
    fun b() {
        println(this.toString())
    }
}

fun test() {
    val t = 33 + A().ss<caret>ssa()
}