package test

abstract class Base(protected var field: Int)

class Derived(value: Int) : Base(value) {
    private val usage: View = object : View() {
        override fun click() {
            val activity = field
        }
    }
}

internal abstract class View {
    internal abstract fun click()
}
