// ERROR: Abstract member cannot be accessed directly
// ERROR: Abstract member cannot be accessed directly
// ERROR: Abstract member cannot be accessed directly
internal interface I {
    val isSomething1: Boolean

    val isSomething2: Boolean?

    val isSomething3: Int

    var isSomething4: Boolean

    var isSomething5: Boolean

    var something6: Boolean
}

internal abstract class C : I {
    override var isSomething1: Boolean
        get() = true
        set(b: Boolean) {
        }

    override var isSomething4: Boolean
        get() = false
        set(value: Boolean) {
            super.isSomething4 = value
        }

    override var isSomething5: Boolean
        get() = super.isSomething5
        set(value: Boolean) {
        }

    override var something6: Boolean
        get() = super.something6
        set(value: Boolean) {
        }
}