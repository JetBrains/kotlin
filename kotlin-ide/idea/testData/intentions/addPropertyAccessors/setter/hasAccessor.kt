// IS_APPLICABLE: false
class Test {
    var x<caret> = 1
        get() = field
        set(value) {
            field = value
        }
}