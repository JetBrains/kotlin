// "Replace with generated @PublishedApi bridge call '`access$prop`'" "true"

open class ABase {
    protected var prop = 1

    inline fun test() {
        {
            <caret>prop
        }()
    }

    @PublishedApi
    internal var `access$prop`: Int
        get() = prop
        set(value) {
            prop = value
        }
}