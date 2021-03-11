// NAME: X
interface T {}

// SIBLING:
class <caret>A : T {
    constructor()

    // INFO: {checked: "true"}
    fun foo() {

    }
}