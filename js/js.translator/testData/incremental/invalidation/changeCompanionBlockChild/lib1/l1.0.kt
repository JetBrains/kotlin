var staticInitParent = false

open class Parent {
    companion {
        val parentValue: Unit = run {
            staticInitParent = true
            Unit
        }
    }
}
