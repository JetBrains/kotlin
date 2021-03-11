// "Safe delete constructor" "false"
// ACTION: Convert to primary constructor
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected

class Ctor {
    <caret>constructor(p: Int)

    fun justCompare() {}
}
