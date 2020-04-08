// ERROR: Property must be initialized
// ERROR: Property must be initialized
// ERROR: Property must be initialized
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
        set(b) {}

    override var isSomething4: Boolean
        get() = false
        set

    override var isSomething5: Boolean
        get
        set(value) {}

    override var something6: Boolean
        get
        set(value) {}
}