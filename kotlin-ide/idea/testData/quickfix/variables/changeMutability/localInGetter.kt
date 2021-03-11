// "Change to var" "true"

val String.prop: Int
    get() {
        val p = 1
        <caret>p = 2
        return p
    }