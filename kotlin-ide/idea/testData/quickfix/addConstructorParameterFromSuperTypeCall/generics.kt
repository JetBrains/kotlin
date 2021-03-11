// "Add constructor parameter 'foos'" "true"
abstract class Foo(foos: List<String>)
class Bar() : Foo(<caret>)