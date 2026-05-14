package test

class Demo(val x: String, val y: String = "default") {
    fun foo() = "foo $x"
    fun useHidden() = hidden()
    private fun hidden() = "hidden changed $x"
    val demoField = "demo field"
}
