// "Implement abstract class" "true"
// WITH_RUNTIME
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

class Container {
    inner abstract class <caret>Base {
        abstract fun foo(): String
    }
}
