// NAME: X
// INFO: {checked: "true"}
interface T {}

open class A

// SIBLING:
class <caret>B : A(), T {
    // INFO: {checked: "true", toAbstract: "true"}
    fun foo() {

    }
}