// "Make 'foo' private" "false"
// ACTION: Convert receiver to parameter
// ACTION: Introduce import alias
// ACTION: Make 'Private' protected
// ACTION: Make 'Private' public
// ERROR: 'protected (in My)' member exposes its 'private' receiver type argument Private

class Receiver<T>

abstract class My {
    private class Private
    // abstract never can be private
    abstract protected fun <caret>Receiver<Private>.foo()
}