package test

class Demo(val x: String, val y: String = "default", val z: Int = 1) {
    fun foo() = "foo changed $x"
    fun useHidden() = hidden()
    private fun hidden() = "hidden changed $x"
    val demoField = "demo field changed"
}
