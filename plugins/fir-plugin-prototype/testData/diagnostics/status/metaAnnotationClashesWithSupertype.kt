import org.jetbrains.kotlin.fir.plugin.AllOpen

@AllOpen
annotation class Open

annotation class Ann

open class Base {
    annotation class Open
    annotation class Ann
}

class Derived : Base() {
    @<!COMPILER_REQUIRED_ANNOTATION_AMBIGUITY!>Open<!> // should be an error
    @Ann // should be ok
    class ShouldBeFinal
}

class ShouldBeAnError : Derived.ShouldBeFinal()
