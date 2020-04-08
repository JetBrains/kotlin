inline class InlineClass(val x: Int)

// Unused
fun foo(arg: InlineClass) {
    arg.x.hashCode()
}