// IS_APPLICABLE: false

private val foo = { x: Int ->
    class Inner() {
      fun temp(<caret>y: Int) : Int { return x + y }
    }
    Inner()
}
