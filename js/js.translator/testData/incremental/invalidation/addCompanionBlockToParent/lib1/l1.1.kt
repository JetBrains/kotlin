var staticInitParent = false

open class Parent {
    val someField: Int = 0
    companion {
        val parentValue: Unit = run {
            staticInitParent = true
            Unit
        }
    }
}
