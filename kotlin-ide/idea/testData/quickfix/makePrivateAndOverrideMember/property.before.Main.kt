// "Make private and overrides 'getName'" "true"
// DISABLE-ERRORS
class B : JavaClass() {
    <caret>val name: String = ""
}