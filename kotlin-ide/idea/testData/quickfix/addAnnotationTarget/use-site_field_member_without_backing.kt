// "Add annotation target" "false"
// ACTION: Introduce import alias
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Specify type explicitly
// ERROR: This annotation is not applicable to target 'member property without backing field or delegate' and use site target '@field'

@Target
annotation class Ann

class Test {
    @field:Ann<caret>
    var bar
        get() = ""
        set(p) {}
}