// "Create abstract function 'bar'" "false"
// ACTION: Create function 'bar'
// ACTION: Rename reference
// ERROR: Unresolved reference: bar
class Foo : Runnable {
    override fun run() {
        <caret>bar()
    }
}