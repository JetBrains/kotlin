// "Initialize with constructor parameter" "true"
open class RGrandAccessor(x: Int) {}

open class RAccessor : RGrandAccessor {
    <caret>val f: Int
    constructor(p: Boolean) : super(1)
    constructor(p: String) : this(true)
}