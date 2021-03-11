var Int.<caret>property: Int
    get() = this
    set(value) {
        println("Set value of $value for $this")
    }

fun foo() {
    println(1.property)
    2.property = 10
}