// "Replace usages of 'OldClass' in whole project" "true"

package ppp

@Deprecated("", ReplaceWith("NewClass"))
open class OldClass(val p: Int) {
    constructor() : this(0)
}

open class NewClass(val p: Int = 0)

class Derived1 : <caret>OldClass(1)

class Derived2 : OldClass {
    constructor(p: Int) : super(p)
    constructor() : super()
}
